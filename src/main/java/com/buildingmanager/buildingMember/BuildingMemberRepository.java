package com.buildingmanager.buildingMember;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuildingMemberRepository extends JpaRepository<BuildingMember, Integer> {

    // Βρες όλα τα μέλη μιας πολυκατοικίας
    List<BuildingMember> findByBuildingId(Integer buildingId);

    // Βρες όλα τα buildings που ανήκει ο χρήστης
    List<BuildingMember> findByUserId(Integer userId);

    // Βρες τον συγκεκριμένο χρήστη σε συγκεκριμένη πολυκατοικία
    Optional<BuildingMember> findByBuilding_IdAndUser_Id(Integer buildingId, Integer userId);
    Optional<BuildingMember> findByBuilding_IdAndRole_Name(Integer buildingId, String roleName);

    Optional<BuildingMember> findFirstByUser_Id(Integer userId);

    @Transactional
    @Modifying
    @Query("delete from BuildingMember bm where bm.building.id = :buildingId")
    void deleteByBuildingId(@Param("buildingId") Integer buildingId);

    boolean existsByBuilding_IdAndUser_Id(Integer buildingId, Integer userId);

    boolean existsByBuilding_IdAndUser_IdAndApartment_Id(Integer buildingId, Integer userId, Integer apartmentId);




}
