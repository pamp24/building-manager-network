package com.buildingmanager.apartment;


import com.buildingmanager.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, Integer> {
    @Query("""
    SELECT apartment
    FROM Apartment apartment
    WHERE apartment.building.id = :buildingId
    ORDER BY apartment.floor ASC, apartment.number ASC
    """)
    Page<Apartment> findAllByBuildingId(@Param("buildingId") Integer buildingId, Pageable pageable);

    // Βρίσκει διαμέρισμα με βάση τον ένοικο
    List<Apartment> findByResident_Id(Integer residentId);

    List<Apartment> findByOwner_Id(Integer ownerId);

    List<Apartment> findAllByBuilding_Id(Integer buildingId);

    List<Apartment> findByOwnerOrResident(User owner, User resident);

    @Query("""
    SELECT a FROM Apartment a
    WHERE a.building.id = :buildingId AND
    (
        (:role = 'Owner' AND a.owner IS NULL
            AND NOT EXISTS (
                SELECT i FROM Invite i
                WHERE i.apartment = a AND i.role = 'Owner' AND i.status = 'PENDING'
            )
        )
        OR
        (:role = 'Resident' AND a.resident IS NULL AND a.isRented = true
            AND NOT EXISTS (
                SELECT i FROM Invite i
                WHERE i.apartment = a AND i.role = 'Resident' AND i.status = 'PENDING'
            )
        )
    )
""")
    List<Apartment> findAvailableApartmentsForRole(
            @Param("buildingId") Integer buildingId,
            @Param("role") String role
    );

    @Transactional
    @Modifying
    @Query("delete from Apartment a where a.building.id = :buildingId")
    void deleteByBuildingId(@Param("buildingId") Integer buildingId);

    @Query("""
    select count(a)
    from Apartment a
    where a.building.id in :buildingIds
      and a.owner is null
      and a.resident is null
""")
    long countUnassignedApartmentsByBuildingIds(@Param("buildingIds") List<Integer> buildingIds);

    long count();

    long countByOwnerIsNotNullOrResidentIsNotNull();

    long countByOwnerIsNull();

    @Query("""
    select count(a) from Apartment a
    where a.owner is not null or a.resident is not null
""")
    long countAssignedApartments();

    @Query("""
    select count(a) from Apartment a
    where a.owner is null and a.resident is null
""")
    long countVacantApartments();

    List<Apartment> findAllByBuilding_IdAndActiveTrueAndEnableTrueOrderByFloorAscNumberAsc(
            Integer buildingId
    );

    boolean existsByBuilding_IdAndFloorIgnoreCaseAndNumberIgnoreCaseAndIdNotAndActiveTrue(
            Integer buildingId,
            String floor,
            String number,
            Integer apartmentId
    );

    boolean existsByBuilding_IdAndParkingSlotIgnoreCaseAndIdNotAndActiveTrue(
            Integer buildingId,
            String parkingSlot,
            Integer apartmentId
    );

    boolean existsByBuilding_IdAndStorageSlotIgnoreCaseAndIdNotAndActiveTrue(
            Integer buildingId,
            String storageSlot,
            Integer apartmentId
    );

    long countByBuilding_IdAndParkingSpaceTrueAndActiveTrue(
            Integer buildingId
    );

    long countByBuilding_IdAndApStorageExistTrueAndActiveTrue(
            Integer buildingId
    );

    boolean existsByBuilding_IdAndFloorIgnoreCaseAndNumberIgnoreCaseAndActiveTrue(
            Integer buildingId,
            String floor,
            String number
    );

    boolean existsByBuilding_IdAndParkingSlotIgnoreCaseAndActiveTrue(
            Integer buildingId,
            String parkingSlot
    );

    boolean existsByBuilding_IdAndStorageSlotIgnoreCaseAndActiveTrue(
            Integer buildingId,
            String storageSlot
    );

    @Query("""
    select a
    from Apartment a
    where a.building.id = :buildingId
      and a.isManagerHouse = true
      and (
           a.owner.id = :userId
           or a.resident.id = :userId
      )
""")
    Optional<Apartment> findManagerApartmentByBuildingIdAndUserId(
            @Param("buildingId") Integer buildingId,
            @Param("userId") Integer userId
    );
    Optional<Apartment> findFirstByBuilding_IdAndIsManagerHouseTrue(
            Integer buildingId
    );
}

