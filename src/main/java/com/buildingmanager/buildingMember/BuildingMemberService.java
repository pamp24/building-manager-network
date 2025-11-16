package com.buildingmanager.buildingMember;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.BuildingMember;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.invite.Invite;
import com.buildingmanager.invite.InviteRepository;
import com.buildingmanager.invite.InviteStatus;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildingMemberService {

    private final BuildingMemberRepository buildingMemberRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApartmentRepository apartmentRepository;
    private final InviteRepository inviteRepository;

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
        List<BuildingMemberDTO> result = new ArrayList<>();
        List<BuildingMember> memberships = buildingMemberRepository.findByBuildingId(buildingId);
        List<Apartment> apartments = apartmentRepository.findAllByBuilding_Id(buildingId);

        // 🔹 1. Κανονικά μέλη από τον πίνακα building_member
        for (BuildingMember m : memberships) {
            apartments.stream()
                    .filter(a ->
                            (a.getOwner() != null && a.getOwner().getId().equals(m.getUser().getId())) ||
                                    (a.getResident() != null && a.getResident().getId().equals(m.getUser().getId()))
                    )
                    .forEach(ap -> result.add(new BuildingMemberDTO(
                            m.getUser().getId(),
                            m.getUser().getFullName(),
                            m.getUser().getEmail(),
                            m.getRole().getName(),
                            "ACCEPTED", // ✅ εμφανίζονται ως ενεργά μέλη
                            m.getBuilding().getId(),
                            m.getBuilding().getName(),
                            ap.getNumber(),
                            ap.getFloor()
                    )));
        }

        // Κρατάμε emails που έχουν ήδη προστεθεί
        Set<String> addedEmails = result.stream()
                .map(BuildingMemberDTO::getEmail)
                .collect(Collectors.toSet());

        // 🔹 2. Προσκλήσεις
        List<Invite> invites = inviteRepository.findByApartment_Building_Id(buildingId);

        for (Invite invite : invites) {
            // ✅ Αν ο χρήστης έχει κάνει ACCEPT και ήδη υπάρχει στα μέλη, μην τον ξαναπροσθέσεις
            if (invite.getStatus() == InviteStatus.ACCEPTED && addedEmails.contains(invite.getEmail())) {
                continue;
            }

            // 🔹 Προσθήκη προσκλήσεων (PENDING ή ACCEPTED χωρίς building_member)
            String fullName = null;
            if (invite.getStatus() == InviteStatus.ACCEPTED) {
                Optional<User> userOpt = userRepository.findByEmail(invite.getEmail());
                fullName = userOpt.map(User::getFullName).orElse(null);
            }

            result.add(new BuildingMemberDTO(
                    null,
                    fullName,
                    invite.getEmail(),
                    invite.getRole(),
                    invite.getStatus().name(), // ✅ “PENDING” ή “ACCEPTED”
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
}
