package com.buildingmanager.commonExpenseItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommonExpenseItemRepository extends JpaRepository<CommonExpenseItem, Integer> {
    List<CommonExpenseItem> findByStatementIdAndActiveTrue(Integer statementId);


    @Query("""
    SELECT new com.buildingmanager.commonExpenseItem.ExpenseCategorySummaryDTO(
        i.category,
        SUM(i.price)
    )
    FROM CommonExpenseItem i
    JOIN i.statement s
    WHERE s.building.id = :buildingId
      AND (
          s.startDate <= :endOfPeriod
          AND (s.endDate IS NULL OR s.endDate >= :startOfPeriod)
      )
    GROUP BY i.category
    ORDER BY SUM(i.price) DESC
""")
    List<ExpenseCategorySummaryDTO> findCategoryTotals(
            @Param("buildingId") Integer buildingId,
            @Param("startOfPeriod") LocalDateTime startOfPeriod,
            @Param("endOfPeriod") LocalDateTime endOfPeriod
    );

}
