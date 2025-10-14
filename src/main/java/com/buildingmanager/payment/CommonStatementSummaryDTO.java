package com.buildingmanager.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonStatementSummaryDTO {
    private Double totalAmount;         // Συνολικό ποσό χρεώσεων
    private Double totalPaid;           // Πόσο έχει πληρωθεί
    private Double totalPending;        // Υπόλοιπο
    private Double paidPercent;         // Ποσοστό πληρωμών
    private LocalDateTime lastDueDate;  // Τελευταία ημερομηνία λήξης
}
