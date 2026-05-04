package com.buildingmanager.propertyAgent;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertyAgentManagementResponse {
    private Integer id;
    private String fullName;
    private String email;
    private List<AgentBuildingResponse> assignedBuildings;
}
