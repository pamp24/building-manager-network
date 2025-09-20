package com.buildingmanager.apartment;


import com.buildingmanager.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
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
    """)
    Page<Apartment> findAllByBuildingId(Integer buildingId, Pageable pageable);

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

}

