package com.buildingmanager.buildingMember;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.invite.Invite;
import com.buildingmanager.invite.InviteRepository;
import com.buildingmanager.invite.InviteStatus;
import com.buildingmanager.notification.NotificationService;
import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;
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



    public BuildingMember addMember(Integer buildingId, Integer userId, Integer roleId, String status) {
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
                .status(status != null ? status : "Joined")
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
                    m.getStatus(),
                    m.getBuilding() != null ? m.getBuilding().getId() : null,
                    m.getBuilding() != null ? m.getBuilding().getName() : null,
                    ap != null ? ap.getNumber() : null,
                    ap != null ? ap.getFloor() : null
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
                    invite.getStatus().name(),
                    invite.getApartment().getBuilding().getId(),
                    invite.getApartment().getBuilding().getName(),
                    invite.getApartment().getNumber(),
                    invite.getApartment().getFloor()
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
                .status("PENDING_APARTMENT")
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
    public void assignApartment(Integer memberId, AssignApartmentRequest req, Authentication auth) {

        User manager = (User) auth.getPrincipal();

        BuildingMember joinMember = buildingMemberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("BuildingMember not found"));

        Integer buildingId = joinMember.getBuilding().getId();

        //check manager
        boolean isManager = buildingMemberRepository.findByBuildingId(buildingId).stream().anyMatch(m ->
                m.getUser().getId().equals(manager.getId())
                        && m.getRole() != null
                        && "BuildingManager".equals(m.getRole().getName())
        );

        if (!isManager) {
            throw new AccessDeniedException("Not allowed");
        }

        //find apartment
        Apartment apartment = apartmentRepository.findById(req.getApartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Apartment not found"));

        if (!apartment.getBuilding().getId().equals(buildingId)) {
            throw new IllegalArgumentException("Apartment does not belong to this building");
        }

        // role name from request
        String roleName = req.getRole() == null ? "" : req.getRole().trim();

        Role roleEntity = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));

        User targetUser = joinMember.getUser();

        //μην επιτρέψεις δεύτερη ανάθεση για ίδιο user+apartment
        boolean alreadyLinked = buildingMemberRepository
                .existsByBuilding_IdAndUser_IdAndApartment_Id(buildingId, targetUser.getId(), apartment.getId());

        if (alreadyLinked) {
            throw new IllegalStateException("User already assigned to this apartment");
        }

        //rules + assign into apartment (owner/resident)
        if ("Owner".equals(roleName)) {
            if (apartment.getOwner() != null) {
                throw new IllegalStateException("Apartment already has owner");
            }
            apartment.setOwner(targetUser);

        } else if ("Resident".equals(roleName)) {
            if (apartment.getResident() != null) {
                throw new IllegalStateException("Apartment already has resident");
            }
            if (!Boolean.TRUE.equals(apartment.getIsRented())) {
                throw new IllegalStateException("Apartment is not rented");
            }
            apartment.setResident(targetUser);

        } else {
            throw new IllegalArgumentException("Invalid role. Use Owner or Resident");
        }

        apartmentRepository.save(apartment);

        // ΔΗΜΙΟΥΡΓΗΣΕ ΝΕΟ BuildingMember για αυτή την ανάθεση
        boolean isFirstAssignmentRow = joinMember.getApartment() == null
                && "PENDING_APARTMENT".equals(joinMember.getStatus());

        //1ο assign: update joinMember (δεν δημιουργώ νέο row)
        if (isFirstAssignmentRow) {

            joinMember.setRole(roleEntity);          // Owner ή Resident
            joinMember.setApartment(apartment);      // A1
            joinMember.setStatus("ACCEPTED");        // ή "JOINED" όπως θες

            buildingMemberRepository.save(joinMember);

        } else {
            //2ο+ assign: δημιουργώ νέο row (κρατάω το παλιό ως έχει)
            BuildingMember assigned = BuildingMember.builder()
                    .building(joinMember.getBuilding())
                    .user(targetUser)
                    .role(roleEntity)
                    .status("Joined")
                    .apartment(apartment)
                    .build();

            buildingMemberRepository.save(assigned);
        }
        // GLOBAL ROLE UPDATE with priority: Owner > Resident

        String currentRole = targetUser.getRole() != null
                ? targetUser.getRole().getName()
                : null;

        boolean alreadyOwner = "Owner".equals(currentRole);

        // Αν γίνει assign Owner → πάντα Owner
        // Αν γίνει assign Resident → ΜΟΝΟ αν ΔΕΝ είναι ήδη Owner
        if ("Owner".equals(roleName) || !alreadyOwner) {
            targetUser.setRole(roleEntity);
            userRepository.save(targetUser);
        }


        //notification στον χρήστη (APARTMENT_ASSIGNED)
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("buildingId", buildingId);
        payloadMap.put("buildingName", joinMember.getBuilding().getName());
        payloadMap.put("apartmentId", apartment.getId());
        payloadMap.put("apartmentFloor", apartment.getFloor());
        payloadMap.put("apartmentNumber", apartment.getNumber());
        payloadMap.put("assignedRole", roleName);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            payloadJson = "{}";
        }

        notificationService.create(
                targetUser,
                "Joined",
                "Apartment was assigned",
                payloadJson
        );
    }

    @Transactional
    public void deleteMember(Integer memberId, Authentication auth) {

        User manager = (User) auth.getPrincipal();

        BuildingMember member = buildingMemberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));

        Integer buildingId = member.getBuilding().getId();

        //check manager rights
        boolean isManager = buildingMemberRepository.findByBuildingId(buildingId)
                .stream()
                .anyMatch(m ->
                        m.getUser().getId().equals(manager.getId()) &&
                                m.getRole() != null &&
                                "BuildingManager".equals(m.getRole().getName())
                );

        if (!isManager) {
            throw new AccessDeniedException("Not allowed");
        }

        //αν υπάρχει apartment, καθάρισε owner/resident
        Apartment apartment = member.getApartment();
        if (apartment != null) {

            if (member.getRole() != null) {
                String roleName = member.getRole().getName();

                if ("Owner".equals(roleName) &&
                        apartment.getOwner() != null &&
                        apartment.getOwner().getId().equals(member.getUser().getId())) {
                    apartment.setOwner(null);
                }

                if ("Resident".equals(roleName) &&
                        apartment.getResident() != null &&
                        apartment.getResident().getId().equals(member.getUser().getId())) {
                    apartment.setResident(null);
                }
            }

            apartmentRepository.save(apartment);
        }

        buildingMemberRepository.delete(member);
    }


}

