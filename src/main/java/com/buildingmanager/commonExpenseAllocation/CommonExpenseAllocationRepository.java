package com.buildingmanager.commonExpenseAllocation;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonExpenseAllocationRepository extends JpaRepository<CommonExpenseAllocation, Integer> {
    // Όλα τα allocations για ένα συγκεκριμένο statement
    List<CommonExpenseAllocation> findByStatement(CommonExpenseStatement statement);

    // Όλα τα allocations για ένα συγκεκριμένο apartment
    List<CommonExpenseAllocation> findByApartment(Apartment apartment);

    // Όλα τα allocations για συγκεκριμένο statement + apartment
    List<CommonExpenseAllocation> findByStatementAndApartment(CommonExpenseStatement statement, Apartment apartment);
    List<CommonExpenseAllocation> findAllByStatement_Id(Integer statementId);

    List<CommonExpenseAllocation> findByItemIdAndActiveTrue(Integer itemId);

}

