package com.buildingmanager.commonExpenseStatement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommonExpenseStatementRepository extends JpaRepository<CommonExpenseStatement, Integer> {

    @Query("SELECT MAX(s.sequenceNumber) FROM CommonExpenseStatement s WHERE s.building.id = :buildingId")
    Integer findMaxSequenceByBuilding(@Param("buildingId") Integer buildingId);

    List<CommonExpenseStatement> findByBuildingId(Integer buildingId);
    @Query("""
SELECT s
FROM CommonExpenseStatement s
WHERE s.building.id = :buildingId
  AND s.status IN ('ISSUED', 'PAID', 'EXPIRED')
ORDER BY s.startDate DESC
""")
    List<CommonExpenseStatement> findActiveByBuildingId(@Param("buildingId") Integer buildingId);


    @Query("SELECT s FROM CommonExpenseStatement s WHERE s.active = true")
    List<CommonExpenseStatement> getAllActive();


    @Query("SELECT s FROM CommonExpenseStatement s WHERE s.building.id = :buildingId")
    List<CommonExpenseStatement> findAllByBuildingIdIncludingInactive(@Param("buildingId") Integer buildingId);


    // ==================== DASHBOARD QUERIES ====================

    @Query("""
    SELECT COUNT(s)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
      AND EXTRACT(MONTH FROM s.startDate) = :month
      AND EXTRACT(YEAR FROM s.startDate) = :year
""")
    long countAllByBuildingMonthYear(@Param("buildingId") Integer buildingId,
                                     @Param("month") int month,
                                     @Param("year") int year);

    @Query("""
    SELECT COUNT(s)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
      AND s.status = :status
      AND EXTRACT(MONTH FROM s.startDate) = :month
      AND EXTRACT(YEAR FROM s.startDate) = :year
""")
    long countByBuildingMonthYearAndStatus(@Param("buildingId") Integer buildingId,
                                           @Param("month") int month,
                                           @Param("year") int year,
                                           @Param("status") StatementStatus status);

    @Query("""
    SELECT COUNT(s)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
""")
    long countAllByBuilding(@Param("buildingId") Integer buildingId);
    @Query("""
    SELECT COUNT(s)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
      AND s.status = :status
""")
    long countByBuildingAndStatus(@Param("buildingId") Integer buildingId,
                                  @Param("status") StatementStatus status);
    @Query("""
    SELECT COUNT(s)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
      AND EXTRACT(MONTH FROM s.startDate) = :month
      AND EXTRACT(YEAR FROM s.startDate) = :year
""")
    long countByBuildingAndCreatedMonthYear(
            @Param("buildingId") Integer buildingId,
            @Param("month") int month,
            @Param("year") int year
    );

    @Query("""
    SELECT COUNT(s)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
      AND s.status IN ('ISSUED', 'PAID', 'EXPIRED')
      AND EXTRACT(MONTH FROM s.startDate) = :month
      AND EXTRACT(YEAR FROM s.startDate) = :year
""")
    long countIssuedExcludingDraftsAndCancelled(
            @Param("buildingId") Integer buildingId,
            @Param("month") int month,
            @Param("year") int year
    );
    @Query("""
    SELECT COUNT(s)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
      AND s.status IN ('ISSUED', 'PAID', 'EXPIRED')
      AND EXTRACT(YEAR FROM s.startDate) = :year
""")
    long countIssuedExcludingDraftsAndCancelledForYear(
            @Param("buildingId") Integer buildingId,
            @Param("year") int year
    );

    @Query("""
    SELECT COUNT(s)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
      AND EXTRACT(MONTH FROM s.startDate) = :month
      AND EXTRACT(YEAR FROM s.startDate) = :year
""")
    long countAllIssuedInMonth(
            @Param("buildingId") Integer buildingId,
            @Param("month") int month,
            @Param("year") int year
    );

    // --- Ληξιπρόθεσμα (unpaid & endDate < now)
    @Query("""
           SELECT COUNT(s)
           FROM CommonExpenseStatement s
           WHERE s.building.id = :buildingId
             AND s.isPaid = FALSE   
             AND s.endDate < :now
           """)
    long countOverdueByBuilding(@Param("buildingId") Integer buildingId, @Param("now") LocalDateTime now);

    // --- Άθροισμα ποσών (εισπραχθέντα)
    @Query("""
           SELECT SUM(a.paidAmount)
           FROM CommonExpenseAllocation a
           WHERE a.statement.building.id = :buildingId
             AND a.isPaid = TRUE
           """)
    Double sumPaidAmountByBuilding(@Param("buildingId") Integer buildingId);

    // --- Άθροισμα ποσών (ανεξόφλητα)
    @Query("""
           SELECT SUM(a.amount - COALESCE(a.paidAmount,0))
           FROM CommonExpenseAllocation a
           WHERE a.statement.building.id = :buildingId
             AND a.isPaid = FALSE
           """)
    Double sumUnpaidAmountByBuilding(@Param("buildingId") Integer buildingId);

    @Query("""
SELECT SUM(s.total)
FROM CommonExpenseStatement s
WHERE s.building.id = :buildingId
  AND EXTRACT(MONTH FROM s.startDate) = :month
  AND EXTRACT(YEAR FROM s.startDate) = :year
  AND s.status IN :statuses
""")
    Double sumByBuildingMonthYearAndStatuses(
            @Param("buildingId") Integer buildingId,
            @Param("month") int month,
            @Param("year") int year,
            @Param("statuses") List<StatementStatus> statuses);


    @Query("""
    SELECT SUM(s.total)
    FROM CommonExpenseStatement s
    WHERE s.building.id = :buildingId
      AND EXTRACT(MONTH FROM s.startDate) = :month
      AND EXTRACT(YEAR FROM s.startDate) = :year
      AND s.status = :status
""")
    Double sumByBuildingMonthYearAndStatus(@Param("buildingId") Integer buildingId,
                                           @Param("month") int month,
                                           @Param("year") int year,
                                           @Param("status") String status);

    @Query("""
    SELECT COALESCE(SUM(i.price), 0)
    FROM CommonExpenseItem i
    JOIN i.statement s
    WHERE s.building.id = :buildingId
      AND MONTH(s.startDate) = :month
""")
    Double sumBuildingExpenses(@Param("buildingId") Integer buildingId,
                               @Param("month") Integer month);

    @Query("""
    SELECT COALESCE(SUM(i.price), 0)
    FROM CommonExpenseItem i
    JOIN i.statement s
    WHERE s.building.id = :buildingId
      AND MONTH(s.startDate) = :month
""")
    Double sumBuildingExpensesByMonth(@Param("buildingId") Integer buildingId,
                                      @Param("month") Integer month);

    @Query("""
    SELECT COALESCE(SUM(i.price), 0)
    FROM CommonExpenseItem i
    JOIN i.statement s
    WHERE s.building.id = :buildingId
      AND YEAR(s.startDate) = :year
""")
    Double sumBuildingExpensesByYear(@Param("buildingId") Integer buildingId,
                                     @Param("year") Integer year);

    List<CommonExpenseStatement> findByBuildingIdOrderByStartDateDesc(Integer buildingId);

}
