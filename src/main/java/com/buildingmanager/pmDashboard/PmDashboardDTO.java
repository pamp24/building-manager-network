package com.buildingmanager.pmDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PmDashboardDTO {
    private SummaryDTO summary;
    private List<AttentionBuildingDTO> attentionBuildings;
    private List<ActivityItemDTO> recentActivity;
    private PendingItemsDTO pendingItems;
    private List<DashboardNotificationDTO> notifications;
}
