package com.buildingmanager.buildingMember;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuildingMemberRepository
        extends JpaRepository<BuildingMember, Integer> {

    // Existing service-compatible methods

    List<BuildingMember> findByBuildingId(Integer buildingId);

    List<BuildingMember> findByUserId(Integer userId);

    Optional<BuildingMember> findByUserIdAndBuildingIdAndStatus(
            Integer userId,
            Integer buildingId,
            BuildingMemberStatus status
    );

    boolean existsByUserIdAndBuildingIdAndStatus(
            Integer userId,
            Integer buildingId,
            BuildingMemberStatus status
    );

    boolean existsByUserIdAndBuildingCompanyIdAndStatus(
            Integer userId,
            Integer companyId,
            BuildingMemberStatus status
    );

    Optional<BuildingMember> findFirstByUserIdAndStatus(
            Integer userId,
            BuildingMemberStatus status
    );

    // Building and user queries

    List<BuildingMember> findByBuilding_Id(Integer buildingId);

    List<BuildingMember> findByUser_Id(Integer userId);

    List<BuildingMember> findByBuilding_IdAndUser_Id(
            Integer buildingId,
            Integer userId
    );

    List<BuildingMember> findByBuilding_IdAndUser_IdAndStatus(
            Integer buildingId,
            Integer userId,
            BuildingMemberStatus status
    );

    Optional<BuildingMember> findByBuilding_IdAndRole_NameAndStatus(
            Integer buildingId,
            String roleName,
            BuildingMemberStatus status
    );

    // Counts

    long countByBuilding_IdInAndStatus(
            List<Integer> buildingIds,
            BuildingMemberStatus status
    );

    long countByBuilding_IdInAndStatusAndRole_NameIn(
            List<Integer> buildingIds,
            BuildingMemberStatus status,
            List<String> roleNames
    );

    // Active membership checks

    boolean existsByBuilding_IdAndUser_IdAndStatusIn(
            Integer buildingId,
            Integer userId,
            List<BuildingMemberStatus> statuses
    );

    boolean existsByBuilding_IdAndUser_IdAndApartment_IdAndStatusAndIdNot(
            Integer buildingId,
            Integer userId,
            Integer apartmentId,
            BuildingMemberStatus status,
            Integer memberId
    );

    // Legacy methods, only where still needed

    boolean existsByBuilding_IdAndUser_Id(
            Integer buildingId,
            Integer userId
    );

    boolean existsByBuilding_IdAndUser_IdAndApartment_Id(
            Integer buildingId,
            Integer userId,
            Integer apartmentId
    );

    // Building cleanup

    @Transactional
    @Modifying
    @Query("""
        delete from BuildingMember bm
        where bm.building.id = :buildingId
    """)
    void deleteByBuildingId(
            @Param("buildingId") Integer buildingId
    );

    @Transactional
    @Modifying
    @Query("""
        update BuildingMember bm
           set bm.status = :status
         where bm.building.id = :buildingId
    """)
    int updateStatusByBuildingId(
            @Param("buildingId") Integer buildingId,
            @Param("status") BuildingMemberStatus status
    );
}