package com.buildingmanager.pmDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteStatsDTO {
    private int totalInvites;
    private int acceptedInvites;
    private int pendingInvites;
    private int rejectedInvites;
    private int acceptanceRate;
}