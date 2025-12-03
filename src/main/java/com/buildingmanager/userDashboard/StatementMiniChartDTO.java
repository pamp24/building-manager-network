package com.buildingmanager.userDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatementMiniChartDTO {
    private double lastAmount;
    private double prevAmount;
    private double percentage; // % διαφορά
    private List<Double> last12;
    private String month;
}
