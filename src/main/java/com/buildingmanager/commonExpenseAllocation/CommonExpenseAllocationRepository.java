package com.buildingmanager.commonExpenseAllocation;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CommonExpenseAllocationRepository extends JpaRepository<CommonExpenseAllocation, Integer> {

    List<CommonExpenseAllocation> findByStatementIdAndApartmentId(Integer statementId, Integer apartmentId);

    @Query("""
    SELECT a
    FROM CommonExpenseAllocation a
    WHERE a.statement.id = :statementId
      AND a.user.id = :userId
""")
    List<CommonExpenseAllocation> findByStatementIdAndUserId(
            @Param("statementId") Integer statementId,
            @Param("userId") Integer userId
    );



    // Όλα τα allocations για συγκεκριμένο statement + apartment
    List<CommonExpenseAllocation> findByStatementAndApartment(CommonExpenseStatement statement, Apartment apartment);
    List<CommonExpenseAllocation> findAllByStatement_Id(Integer statementId);


    @Query("""
    SELECT COALESCE(SUM(a.amount), 0)
    FROM CommonExpenseAllocation a
    JOIN a.item i
    JOIN a.item.statement s
    WHERE a.apartment.id = :apartmentId
      AND YEAR(s.startDate) = :year
""")
    BigDecimal sumApartmentExpensesByYear(@Param("apartmentId") Integer apartmentId,
                                          @Param("year") Integer year);


    @Query("""
        SELECT COALESCE(SUM(a.amount - COALESCE(a.paidAmount, 0)), 0)
        FROM CommonExpenseAllocation a
        WHERE a.apartment.id = :apartmentId
          AND a.isPaid = FALSE
    """)
    BigDecimal sumUnpaidByApartment(@Param("apartmentId") Integer apartmentId);

    @Query("""
select (count(a.id) > 0)
from CommonExpenseAllocation a
where a.statement.id = :statementId
  and coalesce(a.paidAmount, 0) > 0
""")
    boolean hasAnyPaymentForStatement(@Param("statementId") Integer statementId);

    @Query("""
    SELECT COALESCE(SUM(a.amount), 0)
    FROM CommonExpenseAllocation a
    JOIN a.item i
    JOIN i.statement s
    WHERE a.apartment.id = :apartmentId
      AND EXTRACT(MONTH FROM s.startDate) = :month
      AND EXTRACT(YEAR FROM s.startDate) = :year
""")
    BigDecimal sumApartmentExpensesByMonthYear(
            @Param("apartmentId") Integer apartmentId,
            @Param("month") int month,
            @Param("year") int year
    );




}

