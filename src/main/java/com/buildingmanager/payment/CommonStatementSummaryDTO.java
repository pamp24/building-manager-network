package com.buildingmanager.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonStatementSummaryDTO {
    private BigDecimal totalAmount;         // Συνολικό ποσό χρεώσεων
    private BigDecimal totalPaid;           // Πόσο έχει πληρωθεί
    private BigDecimal totalPending;        // Υπόλοιπο
    private BigDecimal paidPercent;         // Ποσοστό πληρωμών
    private LocalDateTime lastDueDate;  // Τελευταία ημερομηνία λήξης
}
