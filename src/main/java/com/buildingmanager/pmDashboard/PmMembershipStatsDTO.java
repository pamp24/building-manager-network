package com.buildingmanager.pmDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PmMembershipStatsDTO {
    private long pendingInvites;
    private long pendingJoinRequests;
    private long joinedMembers;
    private long unassignedApartments;
}