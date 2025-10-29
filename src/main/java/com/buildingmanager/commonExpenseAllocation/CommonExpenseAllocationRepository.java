package com.buildingmanager.commonExpenseAllocation;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommonExpenseAllocationRepository extends JpaRepository<CommonExpenseAllocation, Integer> {

    List<CommonExpenseAllocation> findByStatementIdAndApartmentId(Integer statementId, Integer apartmentId);

    // --- Για έναν μήνα ---
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

    // --- Για ολόκληρο το έτος ---
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
    SELECT a
    FROM CommonExpenseAllocation a
    WHERE a.statement.id = :statementId
      AND a.user.id = :userId
""")
    List<CommonExpenseAllocation> findByStatementIdAndUserId(
            @Param("statementId") Integer statementId,
            @Param("userId") Integer userId
    );

    @Query("""
    SELECT a FROM CommonExpenseAllocation a
    WHERE a.statement.id = :statementId
    AND (a.apartment.owner IS NULL AND a.apartment.resident IS NULL)
""")
    List<CommonExpenseAllocation> findByStatementIdAndUserNull(@Param("statementId") Integer statementId);


    // Όλα τα allocations για συγκεκριμένο statement + apartment
    List<CommonExpenseAllocation> findByStatementAndApartment(CommonExpenseStatement statement, Apartment apartment);
    List<CommonExpenseAllocation> findAllByStatement_Id(Integer statementId);


}

