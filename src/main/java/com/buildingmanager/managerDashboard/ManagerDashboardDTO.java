package com.buildingmanager.managerDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardDTO {

    private long totalIssued;      // Εκδοθέντα
    private long totalPaid;        // Πληρωμένα
    private long totalPending;     // Εκκρεμή
    private long totalExpired;     // Ληξιπρόθεσμα
    private BigDecimal totalIncome;    // Σύνολο Εισπράξεων
    private BigDecimal totalDebt;      // Οφειλές
    private long totalCancelled;   // Ακυρωμένα
    private long totalDraft;       // Πρόχειρα

    private String buildingName;
    private Integer buildingId;

    private List<MonthlyStatsDTO> monthlyStats;
    private List<MonthlyAmountStatsDTO> monthlyAmountStats;

}
