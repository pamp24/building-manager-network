package com.buildingmanager.invite;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.buildingMember.BuildingMember;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.company.Company;
import com.buildingmanager.email.EmailService;
import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteRepository inviteRepository;
    private final ApartmentRepository apartmentRepository;
    private final EmailService emailService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BuildingMemberRepository buildingMemberRepository;

    // CREATE INVITE
    public Invite createInvite(String email, String role, Integer apartmentId, User inviter) {

        if ("PropertyAgent".equalsIgnoreCase(role)) {
            return createPropertyAgentInvite(email, inviter);
        }

        return createApartmentInvite(email, role, apartmentId, inviter);
    }

    // APARTMENT INVITE
    private Invite createApartmentInvite(String email, String role, Integer apartmentId, User inviter) {

        if (apartmentId == null) {
            throw new IllegalArgumentException("apartmentId is null!");
        }

        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        // Owner validation
        if ("Owner".equals(role)) {
            if (apartment.getOwner() != null) {
                throw new RuntimeException("Το διαμέρισμα έχει ήδη Ιδιοκτήτη");
            }
            if (inviteRepository.existsByApartmentIdAndRoleAndStatus(apartmentId, "Owner", InviteStatus.PENDING)) {
                throw new RuntimeException("Υπάρχει ήδη ενεργή πρόσκληση για Owner");
            }
        }

        // Resident validation
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
                .company(null)
                .inviter(inviter)
                .build();

        inviteRepository.save(invite);

        sendInviteEmail(invite, inviter);

        return invite;
    }

    // PROPERTY AGENT INVITE
    private Invite createPropertyAgentInvite(String email, User inviter) {

        Company company = inviter.getCompany();

        if (company == null) {
            throw new RuntimeException("Ο inviter δεν ανήκει σε εταιρία");
        }

        if (inviteRepository.existsByEmailAndRoleAndStatus(email, "PropertyAgent", InviteStatus.PENDING)) {
            throw new RuntimeException("Υπάρχει ήδη ενεργή πρόσκληση για Property Agent");
        }

        Invite invite = Invite.builder()
                .email(email)
                .role("PropertyAgent")
                .apartment(null)
                .company(company)
                .inviter(inviter)
                .build();

        inviteRepository.save(invite);

        sendInviteEmail(invite, inviter);

        return invite;
    }

    // ACCEPT INVITE
    public Invite acceptInvite(String token, String authenticatedEmail) {

        Invite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invite not found"));

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(InviteStatus.EXPIRED);
            inviteRepository.save(invite);
            throw new RuntimeException("Invite expired");
        }

        String inviteEmail = invite.getEmail() != null ? invite.getEmail().trim() : null;
        String authEmail = authenticatedEmail != null ? authenticatedEmail.trim() : null;

        System.out.println("INVITE EMAIL = [" + inviteEmail + "]");
        System.out.println("AUTH EMAIL   = [" + authEmail + "]");

        if (inviteEmail == null || authEmail == null || !inviteEmail.equalsIgnoreCase(authEmail)) {
            throw new RuntimeException("Invite email does not match authenticated user");
        }

        User user = userRepository.findByEmail(inviteEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + inviteEmail));

        Role role = roleRepository.findByName(invite.getRole())
                .orElseThrow(() -> new RuntimeException("Role not found: " + invite.getRole()));

        user.setRole(role);

        switch (invite.getRole()) {
            case "Owner" -> {
                Apartment apartment = invite.getApartment();
                apartment.setOwner(user);
                userRepository.save(user);
                apartmentRepository.save(apartment);
                saveBuildingMember(invite, user, role);
            }

            case "Resident" -> {
                Apartment apartment = invite.getApartment();
                apartment.setResident(user);
                userRepository.save(user);
                apartmentRepository.save(apartment);
                saveBuildingMember(invite, user, role);
            }

            case "BuildingManager" -> {
                Apartment apartment = invite.getApartment();
                apartment.getBuilding().setManager(user);
                userRepository.save(user);
                apartmentRepository.save(apartment);
                saveBuildingMember(invite, user, role);
            }

            case "PropertyAgent" -> {
                user.setCompany(invite.getCompany());
                userRepository.save(user);
            }

            default -> throw new RuntimeException("Unknown role");
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        return inviteRepository.save(invite);
    }

    // BUILDING MEMBER
    private void saveBuildingMember(Invite invite, User user, Role role) {

        Building building = invite.getApartment().getBuilding();

        BuildingMember member = new BuildingMember();
        member.setBuilding(building);
        member.setUser(user);
        member.setRole(role);
        member.setApartment(invite.getApartment());
        member.setStatus(BuildingMemberStatus.JOINED);

        buildingMemberRepository.save(member);
    }

    // EMAIL
    private void sendInviteEmail(Invite invite, User inviter) {

        String inviteLink = "http://localhost:4200/invite/accept?code=" + invite.getToken();

        String subject = switch (invite.getRole()) {
            case "PropertyAgent" -> "Πρόσκληση ως Support Agent";
            case "Owner" -> "Πρόσκληση ως Ιδιοκτήτης";
            case "Resident" -> "Πρόσκληση ως Ένοικος";
            case "BuildingManager" -> "Πρόσκληση ως Διαχειριστής";
            default -> "Πρόσκληση στην εφαρμογή";
        };

        String roleLabel = switch (invite.getRole()) {
            case "PropertyAgent" -> "Support Agent";
            case "Owner" -> "Ιδιοκτήτης";
            case "Resident" -> "Ένοικος";
            case "BuildingManager" -> "Διαχειριστής";
            default -> invite.getRole();
        };

        String contextName = null;

        if (invite.getApartment() != null && invite.getApartment().getBuilding() != null) {
            contextName = "Πολυκατοικία: " + invite.getApartment().getBuilding().getName();
        }

        if (invite.getCompany() != null) {
            contextName = "Εταιρία: " + invite.getCompany().getCompanyName();
        }

        try {
            emailService.sendInviteEmail(
                    invite.getEmail(),
                    inviter.getFullName(),
                    inviteLink,
                    subject,
                    roleLabel,
                    contextName
            );
        } catch (MessagingException e) {
            throw new RuntimeException("Αποτυχία αποστολής πρόσκλησης", e);
        }
    }

    // DTO
    public InviteResponseDTO toDTO(Invite invite) {
        return new InviteResponseDTO(
                invite.getEmail(),
                invite.getRole(),
                invite.getApartment() != null ? invite.getApartment().getId() : null,
                invite.getToken(),
                invite.getStatus().name()
        );
    }
}