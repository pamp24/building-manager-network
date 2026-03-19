package com.buildingmanager.pmDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PmBuildingManagerRowDTO {
    private Integer buildingId;
    private String buildingName;
    private String buildingCode;
    private String city;

    private Integer managerId;
    private String managerFullName;
    private String managerEmail;
    private String managerPhone;

    private boolean managerAssigned;
}
