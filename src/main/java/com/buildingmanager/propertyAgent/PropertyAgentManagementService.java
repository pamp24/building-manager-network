package com.buildingmanager.propertyAgent;

import java.util.List;

public interface PropertyAgentManagementService {
    List<PropertyAgentManagementResponse> getMyCompanyAgents();
    PropertyAgentManagementResponse updateAgentBuildings(Integer agentId, UpdatePropertyAgentBuildingsRequest request);
    void removeAgent(Integer agentId);
}
