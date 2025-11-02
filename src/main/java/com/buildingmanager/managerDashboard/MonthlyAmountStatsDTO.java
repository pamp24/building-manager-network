package com.buildingmanager.managerDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyAmountStatsDTO {
    private String month;
    private Double issuedAmount;
    private Double pendingAmount;
    private Double paidAmount;
    private Double expiredAmount;
}
