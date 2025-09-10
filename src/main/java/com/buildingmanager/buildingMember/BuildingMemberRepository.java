package com.buildingmanager.buildingMember;

import com.buildingmanager.building.BuildingMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuildingMemberRepository extends JpaRepository<BuildingMember, Integer> {

    // Βρες όλα τα μέλη μιας πολυκατοικίας
    List<BuildingMember> findByBuildingId(Integer buildingId);

    // Βρες όλα τα buildings που ανήκει ο χρήστης
    List<BuildingMember> findByUserId(Integer userId);

    // Βρες τον συγκεκριμένο χρήστη σε συγκεκριμένη πολυκατοικία
    Optional<BuildingMember> findByBuilding_IdAndUser_Id(Integer buildingId, Integer userId);

    // Βρες όλα τα μέλη με συγκεκριμένο ρόλο σε μια πολυκατοικία
    List<BuildingMember> findByBuilding_IdAndRole_Name(Integer buildingId, String roleName);
}
