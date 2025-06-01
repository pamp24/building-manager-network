package com.buildingmanager.apartment;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;



@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, Integer> {
    @Query("""
    SELECT apartment
    FROM Apartment apartment
    WHERE apartment.building.id = :buildingId
    """)
    Page<Apartment> findAllByBuildingId(Integer buildingId, Pageable pageable);
}
