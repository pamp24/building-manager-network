package com.buildingmanager.invite;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.user.User;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.buildingmanager.email.EmailService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteRepository inviteRepository;
    private final ApartmentRepository apartmentRepository;
    private final EmailService emailService;

    public Invite createInvite(String email, String role, Integer apartmentId, User inviter) {
        if (apartmentId == null) {
            throw new IllegalArgumentException("apartmentId is null!");
        }

        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        // Validation: μόνο ένας Owner ανά διαμέρισμα
        if ("Owner".equals(role)) {
            if (apartment.getOwner() != null) {
                throw new RuntimeException("Το διαμέρισμα έχει ήδη Ιδιοκτήτη");
            }
            if (inviteRepository.existsByApartmentIdAndRoleAndStatus(apartmentId, "Owner", InviteStatus.PENDING)) {
                throw new RuntimeException("Υπάρχει ήδη ενεργή πρόσκληση για Owner");
            }
        }

            // Validation: μόνο ένας Resident ανά διαμέρισμα
        if ("Resident".equals(role)) {
            if (apartment.getResident() != null) {
                throw new RuntimeException("Το διαμέρισμα έχει ήδη Ένοικο");
            }
            if (!Boolean.TRUE.equals(apartment.getIsRented())) {
                throw new RuntimeException("Το διαμέρισμα δεν είναι προς ενοικίαση");
            }
            if (inviteRepository.existsByApartmentIdAndRoleAndStatus(apartmentId, "Resident", InviteStatus.PENDING)) {
                throw new RuntimeException("Υπάρχει ήδη ενεργή πρόσκληση για Resident");
            }
        }

        Invite invite = Invite.builder()
                .email(email)
                .role(role)
                .apartment(apartment)
                .inviter(inviter)
                .build();

        inviteRepository.save(invite);

        String inviteLink = "http://localhost:4200/invite/accept?code=" + invite.getToken();
        try {
            emailService.sendInviteEmail(invite.getEmail(), inviter.getFullName(), inviteLink);
        } catch (MessagingException e) {
            throw new RuntimeException("Αποτυχία αποστολής πρόσκλησης", e);
        }

        return invite;

    }


    public Invite acceptInvite(String token, User user) {
        Invite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invite not found"));

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invite expired");
        }
        if (!invite.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("Invite email does not match user");
        }

        switch (invite.getRole()) {
            case "Owner" -> invite.getApartment().setOwner(user);
            case "Resident" -> invite.getApartment().setResident(user);
            case "BuildingManager" -> invite.getApartment().getBuilding().setManager(user);
            default -> throw new RuntimeException("Unknown role");
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        return inviteRepository.save(invite);
    }


    public InviteResponseDTO toDTO(Invite invite) {
        return new InviteResponseDTO(
                invite.getEmail(),
                invite.getRole(),
                invite.getApartment().getId(),
                invite.getToken(),
                invite.getStatus().name()
        );
    }

}
