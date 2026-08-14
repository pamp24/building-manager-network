package com.buildingmanager.user;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.audit.AuditAction;
import com.buildingmanager.audit.Auditable;
import com.buildingmanager.email.EmailService;
import com.buildingmanager.role.Role;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final EmailService emailService;

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    @Auditable(action = AuditAction.PERMISSION_CHANGE)
    public boolean updateUserRole(Integer userId, Role newRole) {
        return userRepository.findById(userId).map(user -> {
            user.setRole(newRole);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public Optional<User> findById(Integer id) {

        return userRepository.findById(id);
    }
    public Optional<User> findByEmail(String email) {

        return userRepository.findByEmail(email);
    }

    public List<UserTableDto> getUsersInSameBuilding(Integer userId) {
        List<Apartment> apartmentsAsResident = apartmentRepository.findByResident_Id(userId);
        List<Apartment> apartmentsAsOwner    = apartmentRepository.findByOwner_Id(userId);
        List<Apartment> all = new ArrayList<>();
        all.addAll(apartmentsAsResident);
        all.addAll(apartmentsAsOwner);

        if (all.isEmpty()) {
            throw new EntityNotFoundException("Δεν βρέθηκε διαμέρισμα για τον χρήστη.");
        }

        Integer buildingId = all.get(0).getBuilding().getId();
        List<Apartment> apartments = apartmentRepository.findAllByBuilding_Id(buildingId);

        return apartments.stream()
                .flatMap(a -> Stream.of(a.getResident(), a.getOwner()))
                .filter(Objects::nonNull)
                .distinct()
                .map(u -> new UserTableDto(
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole().getName(),
                        "Joined"
                ))
                .toList();
    }



    public void inviteUserToBuilding(String email, Integer buildingId, Authentication auth) {
        User inviter = (User) auth.getPrincipal();

        //ο inviter έχει διαμέρισμα στο συγκεκριμένο building
        boolean belongsToBuilding = apartmentRepository
                .findByOwnerOrResident(inviter, inviter)
                .stream()
                .anyMatch(ap -> ap.getBuilding().getId().equals(buildingId));

        if (!belongsToBuilding) {
            throw new EntityNotFoundException("Ο χρήστης δεν ανήκει στην πολυκατοικία " + buildingId);
        }

        String inviteLink =
                "http://localhost:4200/invite-accept?email=" + email + "&buildingId=" + buildingId;

        try {
            emailService.sendInviteEmail(email, inviter.getFullName(), inviteLink);
        } catch (MessagingException e) {
            throw new RuntimeException("Αποτυχία αποστολής email", e);
        }
    }

    @Transactional
    public String uploadProfileImage(MultipartFile file, Integer userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }

        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String extension = extractImageExtension(originalFilename);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String fileName = UUID.randomUUID() + extension;
        Path userImageDir = Paths.get("uploads", "profile-images", String.valueOf(userId));

        try {
            Files.createDirectories(userImageDir);

            deleteOldProfileImage(user);

            Path filePath = userImageDir.resolve(fileName);
            log.debug("Saving file to: {}", filePath.toAbsolutePath());

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/uploads/profile-images/" + userId + "/" + fileName;

            log.debug("Returning imageUrl: {}", imageUrl);

            user.setProfileImageUrl(imageUrl);
            userRepository.save(user);

            return imageUrl;

        }
        catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }

    }

    private String extractImageExtension(String fileName) {
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

    private void deleteOldProfileImage(User user) {
        String currentUrl = user.getProfileImageUrl();
        if (currentUrl == null || currentUrl.isBlank() || !currentUrl.startsWith("/uploads/profile-images/")) {
            return;
        }

        String relativePath = currentUrl.substring("/uploads/".length());
        Path root = Paths.get("uploads").toAbsolutePath().normalize();
        Path filePath = root.resolve(relativePath).normalize();

        if (!filePath.startsWith(root)) {
            return;
        }

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Δεν ήταν δυνατή η διαγραφή της παλιάς φωτογραφίας: {}", filePath);
        }
    }

    @Transactional
    public void updateProfessionalsFavoritesOnly(
            Integer userId,
            boolean enabled
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow();

        user.setProfessionalsFavoritesOnly(enabled);

        userRepository.save(user);
    }


}
