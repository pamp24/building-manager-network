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
public class PmFinancialChartDTO {
    private List<String> labels;
    private List<Double> issued;
    private List<Double> paid;
    private List<Double> overdue;
}
