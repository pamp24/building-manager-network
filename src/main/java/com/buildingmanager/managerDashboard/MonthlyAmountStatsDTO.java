package com.buildingmanager.managerDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyAmountStatsDTO {
    private String month;
    private BigDecimal issuedAmount;
    private BigDecimal pendingAmount;
    private BigDecimal paidAmount;
    private BigDecimal expiredAmount;
}
