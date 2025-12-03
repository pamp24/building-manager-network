package com.buildingmanager.commonExpenseAllocation;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
      AND MONTH(s.startDate) = :month
""")
    Double sumApartmentExpensesByMonth(@Param("apartmentId") Integer apartmentId,
                                       @Param("month") Integer month);


    @Query("""
    SELECT COALESCE(SUM(a.amount), 0)
    FROM CommonExpenseAllocation a
    JOIN a.item i
    JOIN a.item.statement s
    WHERE a.apartment.id = :apartmentId
      AND YEAR(s.startDate) = :year
""")
    Double sumApartmentExpensesByYear(@Param("apartmentId") Integer apartmentId,
                                      @Param("year") Integer year);


    @Query("""
        SELECT COALESCE(SUM(a.amount - COALESCE(a.paidAmount, 0)), 0)
        FROM CommonExpenseAllocation a
        WHERE a.apartment.id = :apartmentId
          AND a.isPaid = FALSE
    """)
    Double sumUnpaidByApartment(@Param("apartmentId") Integer apartmentId);
}

