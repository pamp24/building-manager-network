package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.building.Building;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommonExpenseStatementRepository extends JpaRepository<CommonExpenseStatement, Integer> {

    @Query("SELECT MAX(s.sequenceNumber) FROM CommonExpenseStatement s WHERE s.building.id = :buildingId")
    Integer findMaxSequenceByBuilding(@Param("buildingId") Integer buildingId);

    Optional<CommonExpenseStatement> findTopByBuildingOrderByIdDesc(Building building);

    List<CommonExpenseStatement> findByBuildingId(Integer buildingId);

    @Query("SELECT s FROM CommonExpenseStatement s WHERE s.active = true")
    List<CommonExpenseStatement> getAllActive();


    // --- DASHBOARD QUERIES ---

    @Query("""
    SELECT COUNT(s)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
      AND EXTRACT(MONTH FROM s.startDate) = :month
      AND EXTRACT(YEAR FROM s.startDate) = :year
""")
    long countByBuildingAndMonthAndYear(
            @Param("buildingId") Integer buildingId,
            @Param("month") int month,
            @Param("year") int year
    );

    @Query("""
    SELECT COUNT(s)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
      AND s.isPaid = TRUE
      AND EXTRACT(MONTH FROM s.startDate) = :month
      AND EXTRACT(YEAR FROM s.startDate) = :year
""")
    long countPaidByBuildingAndMonthAndYear(
            @Param("buildingId") Integer buildingId,
            @Param("month") int month,
            @Param("year") int year
    );

    @Query("""
    SELECT COUNT(s)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
      AND s.isPaid = FALSE
      AND EXTRACT(MONTH FROM s.startDate) = :month
      AND EXTRACT(YEAR FROM s.startDate) = :year
""")
    long countUnpaidByBuildingAndMonthAndYear(
            @Param("buildingId") Integer buildingId,
            @Param("month") int month,
            @Param("year") int year
    );
    @Query("""
           SELECT COUNT(s)
           FROM CommonExpenseStatement s
           WHERE s.building.id = :buildingId
             AND s.isPaid = FALSE
             AND s.endDate < :now
           """)
    long countOverdueByBuilding(@Param("buildingId") Integer buildingId, @Param("now") LocalDateTime now);

    // --- SUMS ---
    @Query("""
           SELECT SUM(a.paidAmount)
           FROM CommonExpenseAllocation a
           WHERE a.statement.building.id = :buildingId
             AND a.isPaid = TRUE
           """)
    Double sumPaidAmountByBuilding(@Param("buildingId") Integer buildingId);

    @Query("""
           SELECT SUM(a.amount - COALESCE(a.paidAmount,0))
           FROM CommonExpenseAllocation a
           WHERE a.statement.building.id = :buildingId
             AND a.isPaid = FALSE
           """)
    Double sumUnpaidAmountByBuilding(@Param("buildingId") Integer buildingId);
}
