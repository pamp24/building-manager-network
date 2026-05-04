package com.buildingmanager.adminDashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApartmentUsageDto {
    private long totalApartments;
    private long assignedApartments;
    private long vacantApartments;
}
