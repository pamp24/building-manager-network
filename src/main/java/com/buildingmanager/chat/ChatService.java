package com.buildingmanager.chat;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.buildingMember.BuildingMember;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif"
    );

    private static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final BuildingRepository buildingRepository;
    private final BuildingMemberRepository buildingMemberRepository;
    private final UserRepository userRepository;
    private final BuildingPermissionService permissionService;

    private User freshUser(Authentication auth) {
        User principal = (User) auth.getPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName());
    }

    @Transactional
    public ConversationDTO getOrCreateBuildingConversation(Integer buildingId, Authentication auth) {
        User user = freshUser(auth);
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found: " + buildingId));

        if (!permissionService.canViewBuilding(user, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις πρόσβαση σε αυτή την πολυκατοικία");
        }

        Conversation conversation = conversationRepository
                .findByTypeAndBuilding_Id(ConversationType.BUILDING, buildingId)
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder()
                            .type(ConversationType.BUILDING)
                            .building(building)
                            .createdAt(LocalDateTime.now())
                            .build();
                    newConv = conversationRepository.save(newConv);
                    addActiveBuildingMembers(newConv, buildingId);
                    return newConv;
                });

        addActiveBuildingMembers(conversation, buildingId);
        return toDTO(conversation, user);
    }

    @Transactional
    public ConversationDTO getOrCreatePrivateConversation(Integer otherUserId, Authentication auth) {
        User user = freshUser(auth);
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + otherUserId));

        if (user.getId().equals(other.getId())) {
            throw new IllegalArgumentException("Δεν μπορείς να στείλεις μήνυμα στον εαυτό σου");
        }

        if (!isAdmin(user) && !shareBuilding(user, other)) {
            throw new AccessDeniedException("Μπορείς να συνομιλείς μόνο με μέλη της ίδιας πολυκατοικίας");
        }

        Conversation conversation = conversationRepository
                .findPrivateBetween(user.getId(), other.getId())
                .orElseGet(() -> {
                    Conversation newConv = Conversation.builder()
                            .type(ConversationType.PRIVATE)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return conversationRepository.save(newConv);
                });

        ensureParticipant(conversation, user);
        ensureParticipant(conversation, other);
        return toDTO(conversation, user);
    }

    public List<ConversationDTO> listConversations(Authentication auth) {
        User user = freshUser(auth);
        return participantRepository.findByUser_Id(user.getId()).stream()
                .map(participant -> toDTO(participant.getConversation(), user))
                .sorted(Comparator.comparing(
                        ConversationDTO::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    public List<ChatMessageDTO> listMessages(Integer conversationId, Authentication auth) {
        User user = freshUser(auth);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        checkAccess(conversation, user);
        return messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId).stream()
                .map(message -> toMessageDTO(message, user))
                .toList();
    }

    @Transactional
    public ChatMessageDTO sendMessage(Integer conversationId, SendMessageRequest request, Authentication auth) {
        User user = freshUser(auth);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        checkAccess(conversation, user);

        String content = request.getContent().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Το μήνυμα δεν μπορεί να είναι κενό");
        }

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(user)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        message = messageRepository.save(message);
        return toMessageDTO(message, user);
    }

    @Transactional
    public ChatMessageDTO sendImageMessage(
            Integer conversationId,
            String caption,
            MultipartFile file,
            Authentication auth
    ) {
        User user = freshUser(auth);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found"));
        checkAccess(conversation, user);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Το αρχείο είναι κενό");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Το αρχείο ξεπερνά το μέγιστο μέγεθος των 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractImageExtension(originalFilename);

        String fileName = UUID.randomUUID() + extension;
        Path imageDir = Paths.get("uploads", "chat", String.valueOf(conversationId));

        try {
            Files.createDirectories(imageDir);
            Path filePath = imageDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Αποτυχία αποθήκευσης της φωτογραφίας", e);
        }

        String imageUrl = "/uploads/chat/" + conversationId + "/" + fileName;
        String content = caption != null ? caption.trim() : "";

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(user)
                .content(content)
                .imageUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .build();
        message = messageRepository.save(message);
        return toMessageDTO(message, user);
    }

    private String extractImageExtension(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("Το αρχείο δεν έχει έγκυρο όνομα");
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("Το αρχείο δεν έχει έγκυρη επέκταση.");
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Ο τύπος αρχείου ." + extension + " δεν υποστηρίζεται.");
        }
        return "." + extension;
    }

    private boolean shareBuilding(User user, User other) {
        List<BuildingMember> myMemberships = buildingMemberRepository.findByUser_Id(user.getId());
        for (BuildingMember membership : myMemberships) {
            if (membership.getStatus() == BuildingMemberStatus.JOINED
                    && membership.getBuilding() != null) {
                boolean shared = buildingMemberRepository
                        .existsByUserIdAndBuildingIdAndStatus(
                                other.getId(),
                                membership.getBuilding().getId(),
                                BuildingMemberStatus.JOINED
                        );
                if (shared) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkAccess(Conversation conversation, User user) {
        if (isAdmin(user)) {
            return;
        }
        boolean participant = participantRepository
                .findByConversation_IdAndUser_Id(conversation.getId(), user.getId())
                .isPresent();
        if (!participant) {
            throw new AccessDeniedException("Δεν είσαι μέλος αυτής της συνομιλίας");
        }
    }

    private void ensureParticipant(Conversation conversation, User user) {
        boolean exists = participantRepository
                .findByConversation_IdAndUser_Id(conversation.getId(), user.getId())
                .isPresent();
        if (!exists) {
            participantRepository.save(ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(user)
                    .joinedAt(LocalDateTime.now())
                    .build());
        }
    }

    private void addActiveBuildingMembers(Conversation conversation, Integer buildingId) {
        buildingMemberRepository.findByBuilding_Id(buildingId).stream()
                .filter(membership -> membership.getStatus() == BuildingMemberStatus.JOINED)
                .forEach(membership -> ensureParticipant(conversation, membership.getUser()));
    }

    private ConversationDTO toDTO(Conversation conversation, User currentUser) {
        String name;
        String avatar;
        List<ParticipantDTO> members;

        if (conversation.getType() == ConversationType.BUILDING) {
            Building building = conversation.getBuilding();
            name = building != null ? building.getName() : "Ομαδική συνομιλία";
            avatar = building != null ? building.getProfileImageUrl() : null;
            members = building != null
                    ? buildingMemberRepository.findByBuilding_Id(building.getId()).stream()
                            .filter(membership -> membership.getStatus() == BuildingMemberStatus.JOINED)
                            .map(membership -> toParticipantDTO(membership.getUser()))
                            .toList()
                    : List.of();
        } else {
            List<ConversationParticipant> participants = participantRepository
                    .findByConversation_Id(conversation.getId());
            ConversationParticipant other = participants.stream()
                    .filter(participant -> !participant.getUser().getId().equals(currentUser.getId()))
                    .findFirst()
                    .orElse(participants.stream().findFirst().orElse(null));
            User otherUser = other != null ? other.getUser() : null;
            name = otherUser != null ? otherUser.getFullName() : "Συνομιλία";
            avatar = otherUser != null ? otherUser.getProfileImageUrl() : null;
            members = participants.stream()
                    .map(participant -> toParticipantDTO(participant.getUser()))
                    .toList();
        }

        ChatMessage last = messageRepository
                .findFirstByConversation_IdOrderByCreatedAtDesc(conversation.getId())
                .orElse(null);

        return ConversationDTO.builder()
                .id(conversation.getId())
                .type(conversation.getType().name())
                .buildingId(conversation.getBuilding() != null ? conversation.getBuilding().getId() : null)
                .name(name)
                .avatar(avatar)
                .lastMessage(last != null ? last.getContent() : null)
                .lastMessageAt(last != null ? last.getCreatedAt() : null)
                .members(members)
                .build();
    }

    private ParticipantDTO toParticipantDTO(User user) {
        return ParticipantDTO.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    private ChatMessageDTO toMessageDTO(ChatMessage message, User currentUser) {
        User sender = message.getSender();
        return ChatMessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(sender.getId())
                .senderName(sender.getFullName())
                .senderAvatar(sender.getProfileImageUrl())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .createdAt(message.getCreatedAt())
                .mine(sender.getId().equals(currentUser.getId()))
                .build();
    }
}
