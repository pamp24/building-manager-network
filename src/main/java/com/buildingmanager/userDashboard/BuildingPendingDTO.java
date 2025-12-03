package com.buildingmanager.userDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public class BuildingPendingDTO{

        double totalUnpaid;
        List<String> unpaidMonths;
}