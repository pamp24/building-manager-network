package com.buildingmanager.pmDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryDTO {
    private Integer totalBuildings;
    private Integer totalApartments;
    private Double pendingAmount;
    private Double overdueAmount;
}
