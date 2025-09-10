package com.buildingmanager.invite;

import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteRepository inviteRepository;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;


    public Invite createInvite(String email, String role, Integer apartmentId, User manager) {
        var apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        Invite invite = Invite.builder()
                .email(email)
                .role(role)
                .apartment(apartment)
                .inviteCode(UUID.randomUUID().toString())
                .status(InviteStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7)) // λήγει σε 7 μέρες
                .invitedBy(manager)
                .build();

        return inviteRepository.save(invite);
    }

    public Invite acceptInvite(String code, User user) {
        Invite invite = inviteRepository.findByInviteCode(code)
                .orElseThrow(() -> new RuntimeException("Invite not found"));

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invite expired");
        }
        if (!invite.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("Invite email does not match user");
        }

        // Σύνδεση με apartment
        switch (invite.getRole()) {
            case "Owner" -> invite.getApartment().setOwner(user);
            case "Resident" -> invite.getApartment().setResident(user);
            case "BuildingManager" -> invite.getApartment().getBuilding().setManager(user);
            default -> throw new RuntimeException("Unknown role");
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        return inviteRepository.save(invite); // ✅ επιστρέφει Invite
    }


    public InviteResponseDTO toDTO(Invite invite) {
        return new InviteResponseDTO(
                invite.getEmail(),
                invite.getRole(),
                invite.getApartment().getId(),
                invite.getInviteCode(),
                invite.getStatus().name()
        );
    }
}
