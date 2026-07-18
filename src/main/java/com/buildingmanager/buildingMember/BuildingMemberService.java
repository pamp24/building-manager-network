package com.buildingmanager.buildingMember;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.invite.Invite;
import com.buildingmanager.invite.InviteRepository;
import com.buildingmanager.invite.InviteStatus;
import com.buildingmanager.notification.NotificationService;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildingMemberService {

    private final ObjectMapper objectMapper;
    private final BuildingMemberRepository buildingMemberRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApartmentRepository apartmentRepository;
    private final InviteRepository inviteRepository;
    private final NotificationService notificationService;
    private final BuildingPermissionService buildingPermissionService;



    public BuildingMember addMember(Integer buildingId, Integer userId, Integer roleId, BuildingMemberStatus status) {
        var building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building not found"));
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        var role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        BuildingMember member = BuildingMember.builder()
                .building(building)
                .user(user)
                .role(role)
                .status(status != null ? status : BuildingMemberStatus.JOINED)
                .build();

        return buildingMemberRepository.save(member);
    }

    public List<BuildingMemberDTO> getMembersByBuilding(Integer buildingId) {

        List<BuildingMember> memberships = buildingMemberRepository.findByBuildingId(buildingId);
        List<BuildingMemberDTO> result = new ArrayList<>();

        for (BuildingMember m : memberships) {

            Apartment ap = m.getApartment(); // μπορεί να είναι null

            result.add(new BuildingMemberDTO(
                    m.getId(), // memberId
                    m.getUser() != null ? m.getUser().getId() : null,
                    m.getUser() != null ? m.getUser().getFullName() : null,
                    m.getUser() != null ? m.getUser().getEmail() : null,
                    m.getRole() != null ? m.getRole().getName() : null,
                    m.getUser() != null ? m.getUser().getProfileImageUrl() : null,
                    m.getStatus() != null ? m.getStatus().name() : null,
                    m.getBuilding() != null ? m.getBuilding().getId() : null,
                    m.getBuilding() != null ? m.getBuilding().getName() : null,
                    ap != null ? ap.getNumber() : null,
                    ap != null ? ap.getFloor() : null,
                    ap != null ? ap.getId() : null
            ));
        }
        //Προσκλήσεις (invites) όπως πριν, χωρίς memberId
        List<Invite> invites = inviteRepository.findByApartment_Building_Id(buildingId);

        Set<String> addedEmails = result.stream()
                .map(BuildingMemberDTO::getEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Invite invite : invites) {
            if (invite.getStatus() == InviteStatus.ACCEPTED && addedEmails.contains(invite.getEmail())) {
                continue;
            }

            String fullName = null;
            if (invite.getStatus() == InviteStatus.ACCEPTED) {
                Optional<User> userOpt = userRepository.findByEmail(invite.getEmail());
                fullName = userOpt.map(User::getFullName).orElse(null);
            }

            result.add(new BuildingMemberDTO(
                    null, // memberId
                    null, // userId
                    fullName,
                    invite.getEmail(),
                    invite.getRole(),
                    null,
                    invite.getStatus().name(),
                    invite.getApartment().getBuilding().getId(),
                    invite.getApartment().getBuilding().getName(),
                    invite.getApartment().getNumber(),
                    invite.getApartment().getFloor(),
                    invite.getApartment().getId()
            ));
        }

        return result;
    }

    public List<BuildingMember> getMembersByUser(Integer userId) {
        return buildingMemberRepository.findByUserId(userId);
    }

    public void removeMember(Integer memberId) {
        buildingMemberRepository.deleteById(memberId);
    }

    @Transactional
    public Integer joinByBuildingCode(String code, Authentication auth) {
        User joiner = getConnectedUser(auth);

        Building building = buildingRepository.findByBuildingCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Invalid building code"));

        boolean alreadyMember = buildingMemberRepository
                .existsByBuilding_IdAndUser_Id(building.getId(), joiner.getId());

        if (alreadyMember) {
            return building.getId();
        }

        Role roleUser = roleRepository.findByName("User")
                .orElseThrow(() -> new EntityNotFoundException("Role User not found"));

        BuildingMember member = BuildingMember.builder()
                .building(building)
                .user(joiner)
                .role(roleUser)
                .status(BuildingMemberStatus.PENDING_APARTMENT)
                .apartment(null)
                .build();

        buildingMemberRepository.save(member);

        //notify BuildingManager of this building
        var managerOpt = buildingMemberRepository
                .findByBuildingId(building.getId())
                .stream()
                .filter(m -> m.getRole() != null && "BuildingManager".equals(m.getRole().getName()))
                .findFirst();

        if (managerOpt.isPresent()) {
            User manager = managerOpt.get().getUser();

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("joinerFirstName", joiner.getFirstName());
            payloadMap.put("joinerLastName", joiner.getLastName());
            payloadMap.put("joinerEmail", joiner.getEmail());
            payloadMap.put("buildingId", building.getId());
            payloadMap.put("buildingName", building.getName());

            String payloadJson;
            try {
                payloadJson = objectMapper.writeValueAsString(payloadMap);
            } catch (Exception e) {
                // fallback ώστε να μη χαθεί notification
                payloadJson = "{}";
            }

            notificationService.create(
                    manager,
                    "PENDING_APARTMENT",
                    "New User" +joiner.getEmail()+ "joined the building and pending for apartment" ,
                    payloadJson
            );
        } else {
            System.out.println("No BuildingManager found for buildingId=" + building.getId());
        }

        return building.getId();
    }

    private User getConnectedUser(Authentication auth) {
        String email = auth.getName(); // συνήθως sub από JWT
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
    }

    @Transactional
    public void assignApartment(
            Integer memberId,
            AssignApartmentRequest req,
            Authentication auth
    ) {
        User currentUser = getConnectedUser(auth);

        BuildingMember member = buildingMemberRepository.findById(memberId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Building member not found")
                );


        if (member.getBuilding() == null) {
            throw new IllegalStateException(
                    "Building member is not connected to a building"
            );
        }

        if (member.getUser() == null) {
            throw new IllegalStateException(
                    "Building member is not connected to a user"
            );
        }

        Integer buildingId = member.getBuilding().getId();

        if (!buildingPermissionService.canManageBuilding(
                currentUser,
                buildingId
        )) {
            throw new AccessDeniedException(
                    "You are not allowed to manage members of this building"
            );
        }

        if (req == null || req.getApartmentId() == null) {
            throw new IllegalArgumentException(
                    "Apartment id is required"
            );
        }

        String requestedRole = normalizeApartmentRole(req.getRole());

        Role newRole = roleRepository.findByName(requestedRole)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Role not found: " + requestedRole
                        )
                );

        Apartment newApartment = apartmentRepository
                .findById(req.getApartmentId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Apartment not found")
                );

        if (newApartment.getBuilding() == null
                || !buildingId.equals(
                newApartment.getBuilding().getId()
        )) {
            throw new IllegalArgumentException(
                    "Apartment does not belong to this building"
            );
        }

        User targetUser = member.getUser();
        Apartment oldApartment = member.getApartment();

        String oldRole = member.getRole() != null
                ? member.getRole().getName()
                : null;

        boolean sameApartment =
                oldApartment != null
                        && oldApartment.getId().equals(newApartment.getId());

        boolean sameRole = requestedRole.equals(oldRole);

        /*
         * Δεν χρειάζεται database update αν δεν άλλαξε τίποτα.
         */
        if (sameApartment && sameRole) {
            return;
        }
        boolean duplicateExists =
                buildingMemberRepository
                        .existsByBuilding_IdAndUser_IdAndApartment_IdAndStatusAndIdNot(
                                buildingId,
                                targetUser.getId(),
                                newApartment.getId(),
                                BuildingMemberStatus.JOINED,
                                member.getId()
                        );

        if (duplicateExists) {
            throw new IllegalStateException(
                    "The user is already actively assigned to this apartment"
            );
        }

        /*
         * Πρώτα ελέγχουμε αν η νέα θέση είναι διαθέσιμη.
         * Επιτρέπουμε τον ίδιο user, επειδή μπορεί να αλλάζει μόνο role
         * ή να διορθώνεται ένα ήδη υπάρχον association.
         */
        validateApartmentAvailability(
                newApartment,
                targetUser,
                requestedRole
        );

//         * Καθαρίζουμε την παλιά σχέση μόνο όταν υπάρχει παλιό apartment.
//         *
//         * Αυτό καλύπτει:
//         * - αλλαγή apartment
//         * - αλλαγή role στο ίδιο apartment
//         * - αλλαγή role και apartment μαζί

        if (oldApartment != null) {
            clearPreviousApartmentAssignment(
                    oldApartment,
                    targetUser,
                    oldRole
            );
        }

        applyApartmentAssignment(
                newApartment,
                targetUser,
                requestedRole
        );

        if (oldApartment != null
                && !oldApartment.getId().equals(newApartment.getId())) {
            apartmentRepository.save(oldApartment);
        }

        apartmentRepository.save(newApartment);

//         * Ενημερώνουμε το ίδιο BuildingMember row.
//         * Δεν δημιουργούμε δεύτερο membership.
        member.setApartment(newApartment);
        member.setRole(newRole);
        member.setStatus(BuildingMemberStatus.JOINED);

        buildingMemberRepository.save(member);

        recalculateGlobalUserRole(targetUser);

        sendApartmentAssignmentNotification(
                targetUser,
                member,
                newApartment,
                requestedRole,
                oldApartment,
                oldRole
        );
    }

    private String normalizeApartmentRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "Role is required"
            );
        }

        String normalizedRole = role.trim();

        if (!"Owner".equals(normalizedRole)
                && !"Resident".equals(normalizedRole)) {
            throw new IllegalArgumentException(
                    "Invalid role. Allowed roles: Owner, Resident"
            );
        }

        return normalizedRole;
    }



    private void validateApartmentAvailability(
            Apartment apartment,
            User targetUser,
            String roleName
    ) {
        if ("Owner".equals(roleName)) {
            User existingOwner = apartment.getOwner();

            if (existingOwner != null
                    && !existingOwner.getId().equals(targetUser.getId())) {
                throw new IllegalStateException(
                        "The selected apartment already has an owner"
                );
            }

            return;
        }

        if ("Resident".equals(roleName)) {
            if (!Boolean.TRUE.equals(apartment.getIsRented())) {
                throw new IllegalStateException(
                        "The selected apartment is not marked as rented"
                );
            }

            User existingResident = apartment.getResident();

            if (existingResident != null
                    && !existingResident.getId().equals(targetUser.getId())) {
                throw new IllegalStateException(
                        "The selected apartment already has a resident"
                );
            }
        }
    }

    private void clearPreviousApartmentAssignment(
            Apartment oldApartment,
            User targetUser,
            String oldRole
    ) {
        if ("Owner".equals(oldRole)
                && oldApartment.getOwner() != null
                && oldApartment.getOwner()
                .getId()
                .equals(targetUser.getId())) {

            oldApartment.setOwner(null);
        }

        if ("Resident".equals(oldRole)
                && oldApartment.getResident() != null
                && oldApartment.getResident()
                .getId()
                .equals(targetUser.getId())) {

            oldApartment.setResident(null);
        }
    }

    private void applyApartmentAssignment(
            Apartment apartment,
            User targetUser,
            String roleName
    ) {
        if ("Owner".equals(roleName)) {
            apartment.setOwner(targetUser);
            return;
        }

        if ("Resident".equals(roleName)) {
            apartment.setResident(targetUser);
        }
    }

    private void sendApartmentAssignmentNotification(
            User targetUser,
            BuildingMember member,
            Apartment newApartment,
            String newRole,
            Apartment oldApartment,
            String oldRole
    ) {
        Map<String, Object> payload = new HashMap<>();

        payload.put(
                "buildingId",
                member.getBuilding().getId()
        );

        payload.put(
                "buildingName",
                member.getBuilding().getName()
        );

        payload.put(
                "apartmentId",
                newApartment.getId()
        );

        payload.put(
                "apartmentFloor",
                newApartment.getFloor()
        );

        payload.put(
                "apartmentNumber",
                newApartment.getNumber()
        );

        payload.put("assignedRole", newRole);

        if (oldApartment != null) {
            payload.put(
                    "previousApartmentId",
                    oldApartment.getId()
            );

            payload.put(
                    "previousApartmentFloor",
                    oldApartment.getFloor()
            );

            payload.put(
                    "previousApartmentNumber",
                    oldApartment.getNumber()
            );

            payload.put("previousRole", oldRole);
        }

        String payloadJson;

        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            payloadJson = "{}";
        }

        boolean wasUpdate = oldApartment != null;

        notificationService.create(
                targetUser,
                wasUpdate
                        ? "APARTMENT_UPDATED"
                        : "APARTMENT_ASSIGNED",
                wasUpdate
                        ? "Your apartment assignment was updated"
                        : "An apartment was assigned to you",
                payloadJson
        );
    }

    private void recalculateGlobalUserRole(User targetUser) {
        String currentRole = targetUser.getRole() != null
                ? targetUser.getRole().getName()
                : null;

        if ("Admin".equals(currentRole)
                || "BuildingManager".equals(currentRole)
                || "PropertyManager".equals(currentRole)
                || "PropertyAgent".equals(currentRole)) {
            return;
        }

        List<BuildingMember> memberships =
                buildingMemberRepository.findByUserId(targetUser.getId());

        boolean hasOwnerMembership = memberships.stream()
                .anyMatch(m ->
                        m.getStatus() == BuildingMemberStatus.JOINED
                                && m.getRole() != null
                                && "Owner".equals(m.getRole().getName())
                );

        boolean hasResidentMembership = memberships.stream()
                .anyMatch(m ->
                        m.getStatus() == BuildingMemberStatus.JOINED
                                && m.getRole() != null
                                && "Resident".equals(m.getRole().getName())
                );

        String roleName = hasOwnerMembership
                ? "Owner"
                : hasResidentMembership
                ? "Resident"
                : "User";

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() ->
                        new EntityNotFoundException("Role not found: " + roleName)
                );

        targetUser.setRole(role);
        userRepository.save(targetUser);
    }

    @Transactional
    public void deleteMember(
            Integer memberId,
            Authentication auth
    ) {
        User currentUser = getConnectedUser(auth);

        BuildingMember member = buildingMemberRepository.findById(memberId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Member not found")
                );

        if (member.getBuilding() == null) {
            throw new IllegalStateException(
                    "Building member is not connected to a building"
            );
        }

        if (member.getUser() == null) {
            throw new IllegalStateException(
                    "Building member is not connected to a user"
            );
        }

        Integer buildingId = member.getBuilding().getId();

        if (!buildingPermissionService.canManageBuilding(
                currentUser,
                buildingId
        )) {
            throw new AccessDeniedException(
                    "You are not allowed to remove members from this building"
            );
        }

        if (member.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException(
                    "You cannot remove your own membership"
            );
        }

        if (member.getStatus() == BuildingMemberStatus.LEFT) {
            return;
        }

        if (member.getRole() != null
                && "BuildingManager".equals(
                member.getRole().getName()
        )) {

            long activeManagerCount =
                    buildingMemberRepository
                            .findByBuildingId(buildingId)
                            .stream()
                            .filter(item ->
                                    item.getStatus()
                                            == BuildingMemberStatus.JOINED
                            )
                            .filter(item ->
                                    item.getRole() != null
                            )
                            .filter(item ->
                                    "BuildingManager".equals(
                                            item.getRole().getName()
                                    )
                            )
                            .count();

            if (activeManagerCount <= 1) {
                throw new IllegalStateException(
                        "The last building manager cannot be removed"
                );
            }
        }

        Apartment apartment = member.getApartment();

        if (apartment != null) {
            String roleName = member.getRole() != null
                    ? member.getRole().getName()
                    : null;

            clearPreviousApartmentAssignment(
                    apartment,
                    member.getUser(),
                    roleName
            );

            apartmentRepository.save(apartment);
        }

        /*
         * Soft delete.
         * Κρατάμε building, apartment, user και role
         * για το ιστορικό των support tickets.
         */
        member.setStatus(BuildingMemberStatus.REMOVED);
        buildingMemberRepository.save(member);

        recalculateGlobalUserRole(member.getUser());
    }




}

