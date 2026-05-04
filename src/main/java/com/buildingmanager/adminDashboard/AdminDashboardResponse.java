package com.buildingmanager.adminDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardResponse {
    private PlatformOverviewDto overview;
    private UserActivityDto userActivity;
    private BuildingActivityDto buildingActivity;
    private ApartmentUsageDto apartmentUsage;
    private OperationalIssuesDto operationalIssues;
    private EngagementStatsDto engagementStats;
    private GrowthStatsDto growthStats;
}