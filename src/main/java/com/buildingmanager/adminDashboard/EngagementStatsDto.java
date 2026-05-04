package com.buildingmanager.adminDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EngagementStatsDto {
    private long announcementsCount;
    private long votingsCount;
    private double participationRate;
    private long invitesSent;
    private long invitesAccepted;
    private long pendingInvites;
}
