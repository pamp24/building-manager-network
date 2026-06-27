package com.buildingmanager.commonExpenseItem;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ExpenseCategorySummaryDTO {
    private ExpenseCategory category;
    private BigDecimal totalAmount;
}

