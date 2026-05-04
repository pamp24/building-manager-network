package com.buildingmanager.adminDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GrowthStatsDto {
    private List<GrowthPointDto> userGrowth;
    private List<GrowthPointDto> buildingGrowth;
    private List<GrowthPointDto> inviteGrowth;
}
