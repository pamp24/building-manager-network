package com.buildingmanager.pmDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PmExpenseCollectionRateDTO {
    private int collectionRate;
    private double issuedAmount;
    private double paidAmount;
    private double overdueAmount;
}

