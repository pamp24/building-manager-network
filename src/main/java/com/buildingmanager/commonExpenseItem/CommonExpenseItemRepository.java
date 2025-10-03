package com.buildingmanager.commonExpenseItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonExpenseItemRepository extends JpaRepository<CommonExpenseItem, Integer> {
    List<CommonExpenseItem> findByStatementIdAndActiveTrue(Integer statementId);
}
