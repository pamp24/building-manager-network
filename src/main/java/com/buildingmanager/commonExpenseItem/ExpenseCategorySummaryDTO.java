package com.buildingmanager.commonExpenseItem;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExpenseCategorySummaryDTO {
    private ExpenseCategory category;
    private Double totalAmount;
}

