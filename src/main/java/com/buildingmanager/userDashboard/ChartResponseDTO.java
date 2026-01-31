package com.buildingmanager.userDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class ChartResponseDTO {
    private List<String> labels;
    private List<Double> values;
    private BigDecimal total;
}
