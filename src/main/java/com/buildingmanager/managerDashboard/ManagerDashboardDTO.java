package com.buildingmanager.managerDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardDTO {

    private long totalIssued;        // Πόσα statements εκδόθηκαν (τρέχον μήνα)
    private long totalPaid;          // Πόσα έχουν εξοφληθεί (τρέχον μήνα)
    private long totalPending;       // Πόσα είναι σε αναμονή (τρέχον μήνα)
    private long totalOverdue;       // Πόσα είναι ληξιπρόθεσμα (γενικά)
    private double totalIncome;      // Πόσα έχει εισπράξει συνολικά
    private double totalDebt;        // Πόσα του χρωστάνε συνολικά

    private String buildingName;     // Προαιρετικά, για ποιο κτίριο είναι το dashboard
    private Integer buildingId;      // Id του κτιρίου

    private List<MonthlyStatsDTO> monthlyStats;
}
