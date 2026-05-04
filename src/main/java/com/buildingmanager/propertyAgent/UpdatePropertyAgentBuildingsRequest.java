package com.buildingmanager.propertyAgent;

import lombok.Data;

import java.util.List;

@Data
public class UpdatePropertyAgentBuildingsRequest {
    private List<Integer> buildingIds;
}