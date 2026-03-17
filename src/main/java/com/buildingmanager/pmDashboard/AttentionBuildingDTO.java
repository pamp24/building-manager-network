package com.buildingmanager.pmDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttentionBuildingDTO {
    private Integer buildingId;
    private String name;
    private String city;
    private String issue;
    private String severity; // danger, warning, info
}
