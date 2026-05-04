package com.buildingmanager.adminDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlatformOverviewDto {
    private long totalBuildings;
    private long totalUsers;
    private long totalCompanies;
    private long totalManagers;
}
