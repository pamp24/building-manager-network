package com.buildingmanager.adminDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperationalIssuesDto {
    private long buildingsWithoutManager;
    private long apartmentsWithoutOwner;
    private long pendingInvites;
}