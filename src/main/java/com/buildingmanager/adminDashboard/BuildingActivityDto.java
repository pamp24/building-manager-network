package com.buildingmanager.adminDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuildingActivityDto {
    private long newBuildingsThisMonth;
    private long totalBuildings;
    private long buildingsWithoutManager;
    private long buildingsWithoutApartments;
}
