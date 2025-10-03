package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.building.Building;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommonExpenseStatementRepository extends JpaRepository<CommonExpenseStatement, Integer> {

    @Query("SELECT MAX(s.sequenceNumber) FROM CommonExpenseStatement s WHERE s.building.id = :buildingId")
    Integer findMaxSequenceByBuilding(@Param("buildingId") Integer buildingId);

    Optional<CommonExpenseStatement> findTopByBuildingOrderByIdDesc(Building building);

    List<CommonExpenseStatement> findByBuildingId(Long buildingId);

    @Query("SELECT s FROM CommonExpenseStatement s WHERE s.active = true")
    List<CommonExpenseStatement> getAllActive();


}
