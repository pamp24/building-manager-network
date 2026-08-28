package com.buildingmanager.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Integer> {


    List<Calendar> findByBuildingIdAndActiveTrue(Integer buildingId);

    long count();

    @Query("""
        SELECT c
        FROM CalendarEntity c
        WHERE c.building.id = :buildingId
          AND c.active = true
        ORDER BY c.createdDate DESC
    """)
    List<Calendar> findByBuildingPinnedFirst(@Param("buildingId") Integer buildingId);

    @Query("""
        SELECT c
        FROM CalendarEntity c
        WHERE c.building.id = :buildingId
        ORDER BY c.createdDate DESC
    """)
    List<Calendar> findByBuildingAllPinnedFirst(@Param("buildingId") Integer buildingId);

    @Query("""
        SELECT c
        FROM CalendarEntity c
        WHERE c.building.id = :buildingId
          AND c.active = true
          AND c.endDate IS NOT NULL
          AND c.endDate < :now
    """)
    List<Calendar> findExpired(@Param("buildingId") Integer buildingId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update CalendarEntity c set c.pinned=false where c.building.id = :buildingId")
    void unpinAll(@Param("buildingId") Integer buildingId);
}
