package com.buildingmanager.building;

import com.buildingmanager.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Integer>, JpaSpecificationExecutor<Building> {
    @Query("""
            SELECT Building 
            FROM Building building
            WHERE building.enable = TRUE 
            AND building.active = TRUE
            """)
    Page<Building> findAllDisplayableBuildings(Pageable pageable, int userId);

    @Query("SELECT bm.building FROM BuildingMember bm WHERE bm.user.id = :userId")
    List<Building> findAllByUserId(@Param("userId") Integer userId);

    @Query("select b.manager from Building b where b.id = :buildingId")
    Optional<User> findManagerByBuildingId(@Param("buildingId") Integer buildingId);
}
