package com.buildingmanager.userDashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatingChartDTO {
    private List<String> labels;
    private List<Double> buildingValues;
    private List<Double> apartmentValues;
    private BigDecimal buildingTotal;
    private BigDecimal apartmentTotal;
}
