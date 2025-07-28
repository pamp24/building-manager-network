package com.buildingmanager.apartment;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    Optional<Apartment> findByResident_Id(Integer residentId);

    Optional<Apartment> findByOwner_Id(Integer ownerId);

    List<Apartment> findAllByBuilding_Id(Integer buildingId);



}
