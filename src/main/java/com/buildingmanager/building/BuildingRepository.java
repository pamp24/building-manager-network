package com.buildingmanager.building;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    List<Building> findByManagerId(Integer managerId);

    Optional<Building> findByBuildingCode(String buildingCode);
    List<Building> findByPropertyManager_Id(Integer propertyManagerId);

    List<Building> findByCompany_Id(Integer companyId);
    List<Building> findByIdIn(List<Integer> ids);
    List<Building> findByCompanyId(Integer companyId);

    long countByManagerIsNull();

    @Query("""
    select count(b) from Building b
    where not exists (
        select a.id from Apartment a where a.building.id = b.id
    )
""")
    long countBuildingsWithoutApartments();

    long count();

    long countByCreatedDateAfter(java.time.LocalDateTime dateTime);

    @Query("""
    SELECT DATE(b.createdDate), COUNT(b)
    FROM Building b
    WHERE b.createdDate >= :fromDate
    GROUP BY DATE(b.createdDate)
    ORDER BY DATE(b.createdDate)
""")
    List<Object[]> countBuildingsGroupedByDate(@Param("fromDate") LocalDateTime fromDate);
}
