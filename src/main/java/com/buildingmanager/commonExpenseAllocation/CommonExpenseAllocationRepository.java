package com.buildingmanager.commonExpenseAllocation;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommonExpenseAllocationRepository extends JpaRepository<CommonExpenseAllocation, Integer> {

    @Query("""
    SELECT alloc
    FROM CommonExpenseAllocation alloc
    WHERE alloc.statement.id = :statementId
      AND (
          alloc.apartment.owner.id = :userId
          OR alloc.apartment.resident.id = :userId
      )
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

