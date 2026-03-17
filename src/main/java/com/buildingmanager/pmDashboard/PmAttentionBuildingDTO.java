package com.buildingmanager.pmDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PmAttentionBuildingDTO {
    private Integer buildingId;
    private String buildingName;
    private String buildingCode;
    private double overdueAmount;
    private int collectionRate;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String reason;
}