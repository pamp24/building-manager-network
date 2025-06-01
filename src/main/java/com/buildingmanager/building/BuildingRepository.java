package com.buildingmanager.building;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface BuildingRepository extends JpaRepository<Building, Integer>, JpaSpecificationExecutor<Building> {
    @Query("""
            SELECT Building 
            FROM Building building
            WHERE building.enable = TRUE 
            AND building.active = TRUE
            """)
    Page<Building> findAllDisplayableBuildings(Pageable pageable, int userId);

}
