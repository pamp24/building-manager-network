package com.buildingmanager.adminDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserActivityDto {
    private long activeUsersToday;
    private long activeUsersThisWeek;
    private long newRegistrationsThisMonth;
}