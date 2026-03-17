package com.buildingmanager.pmDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingItemsDTO {
    private Integer pendingInvites;
    private Integer joinRequests;
    private Integer membersWithoutApartment;
}