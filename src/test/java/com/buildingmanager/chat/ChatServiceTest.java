package com.buildingmanager.chat;

import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    @Mock ConversationRepository conversations;
    @Mock ConversationParticipantRepository participants;
    @Mock ChatMessageRepository messages;
    @Mock BuildingRepository buildings;
    @Mock BuildingMemberRepository memberships;
    @Mock UserRepository users;
    @Mock BuildingPermissionService permissions;
    @Mock Authentication auth;
    ChatService service;
    User user = User.builder().id(1).build();
    User other = User.builder().id(2).build();
    Conversation conversation = Conversation.builder().id(10).type(ConversationType.PRIVATE).build();
    ConversationParticipant participant = ConversationParticipant.builder().conversation(conversation).user(user).build();

    @BeforeEach
    void setup() {
        service = new ChatService(conversations, participants, messages, buildings, memberships, users, permissions);
        when(auth.getPrincipal()).thenReturn(user);
        when(users.findById(1)).thenReturn(Optional.of(user));
    }

    @Test
    void hideOnlyChangesCallerAndEmptyPrivateIsNotListed() {
        when(conversations.findForUpdate(10)).thenReturn(Optional.of(conversation));
        when(participants.findByConversation_IdAndUser_Id(10, 1)).thenReturn(Optional.of(participant));
        service.hideConversation(10, auth);
        assertThat(participant.getHiddenAt()).isNotNull();
        when(participants.findByUser_Id(1)).thenReturn(List.of(participant));
        assertThat(service.listConversations(auth)).isEmpty();
        participant.setHiddenAt(null);
        assertThat(service.listConversations(auth)).isEmpty();
    }

    @Test
    void sendingReopensParticipants() {
        ConversationParticipant recipient = ConversationParticipant.builder().conversation(conversation).user(other)
                .hiddenAt(LocalDateTime.now()).build();
        participant.setHiddenAt(LocalDateTime.now());
        when(conversations.findForUpdate(10)).thenReturn(Optional.of(conversation));
        when(participants.findByConversation_IdAndUser_Id(10, 1)).thenReturn(Optional.of(participant));
        when(participants.findByConversation_Id(10)).thenReturn(List.of(participant, recipient));
        when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.sendMessage(10, SendMessageRequest.builder().content("hello").build(), auth);
        assertThat(participant.getHiddenAt()).isNull();
        assertThat(recipient.getHiddenAt()).isNull();
    }

    @Test
    void cannotEditAnotherSendersMessage() {
        when(conversations.findForUpdate(10)).thenReturn(Optional.of(conversation));
        when(participants.findByConversation_IdAndUser_Id(10, 1)).thenReturn(Optional.of(participant));
        when(messages.findByIdAndConversation_Id(5, 10)).thenReturn(Optional.of(
                ChatMessage.builder().conversation(conversation).sender(other).content("hello").build()));
        EditMessageRequest request = new EditMessageRequest();
        request.setContent("changed");
        assertThatThrownBy(() -> service.editMessage(10, 5, request, auth)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteTombstonesAttachmentAndRejectsFurtherEdits() {
        ChatMessage message = ChatMessage.builder().conversation(conversation).sender(user)
                .content("caption").imageUrl("/uploads/chat/10/image.png").build();
        when(conversations.findForUpdate(10)).thenReturn(Optional.of(conversation));
        when(participants.findByConversation_IdAndUser_Id(10, 1)).thenReturn(Optional.of(participant));
        when(messages.findByIdAndConversation_Id(5, 10)).thenReturn(Optional.of(message));
        ChatMessageDTO dto = service.deleteMessage(10, 5, auth);
        assertThat(dto.getDeletedAt()).isNotNull();
        assertThat(dto.getImageUrl()).isNull();
        assertThat(dto.getContent()).isEmpty();
        EditMessageRequest request = new EditMessageRequest();
        request.setContent("changed");
        assertThatThrownBy(() -> service.editMessage(10, 5, request, auth)).isInstanceOf(IllegalArgumentException.class);
    }
}
