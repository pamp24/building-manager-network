package com.buildingmanager.managerDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyStatsDTO {
    private String month;     // π.χ. "Ιαν", "Φεβ", "Μαρ"
    private long issued;      // πόσα statements εκδόθηκαν
    private long pending;
    private long paid;        // πόσα statements εξοφλήθηκαν
    private long expired;

}
