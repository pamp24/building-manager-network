package com.buildingmanager.building;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BuildingRepository extends JpaRepository<Building, Integer>, JpaSpecificationExecutor<Building> {
    @Query("""
            SELECT Building 
            FROM Building building
            WHERE building.enable = TRUE 
            AND building.active = TRUE
            """)
    Page<Building> findAllDisplayableBuildings(Pageable pageable, int userId);

    @Query("" +
            "SELECT b " +
            "FROM Building b " +
            "JOIN b.users u " +
            "WHERE u.id = :userId")
    List<Building> findAllByUserId(@Param("userId") Integer userId);

}
