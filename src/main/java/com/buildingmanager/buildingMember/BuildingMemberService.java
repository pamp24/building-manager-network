package com.buildingmanager.buildingMember;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.BuildingMember;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingMemberService {

    private final BuildingMemberRepository buildingMemberRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApartmentRepository apartmentRepository;

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

        return memberships.stream()
                .flatMap(m -> {
                    // Φέρε apartments αυτής της πολυκατοικίας
                    List<Apartment> apartments = apartmentRepository.findAllByBuilding_Id(buildingId);

                    // Φιλτράρουμε τα apartments που ανήκουν στον user
                    return apartments.stream()
                            .filter(ap ->
                                    (ap.getOwner() != null && ap.getOwner().getId().equals(m.getUser().getId())) ||
                                            (ap.getResident() != null && ap.getResident().getId().equals(m.getUser().getId()))
                            )
                            .map(ap -> new BuildingMemberDTO(
                                    m.getUser().getId(),
                                    m.getUser().getFullName(),
                                    m.getUser().getEmail(),
                                    m.getRole().getName(),
                                    m.getStatus(),
                                    m.getBuilding().getId(),
                                    m.getBuilding().getName(),
                                    ap.getNumber(),   // 👈 ελέγχεις πώς ονομάζεται το πεδίο στο Apartment
                                    ap.getFloor()     // 👈 ελέγχεις πώς ονομάζεται το πεδίο στο Apartment
                            ));
                })
                .toList();
    }




    public List<BuildingMember> getMembersByUser(Integer userId) {
        return buildingMemberRepository.findByUserId(userId);
    }

    public void removeMember(Integer memberId) {
        buildingMemberRepository.deleteById(memberId);
    }
}
