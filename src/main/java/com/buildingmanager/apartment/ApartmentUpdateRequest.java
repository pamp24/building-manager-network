package com.buildingmanager.apartment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentUpdateRequest {

    private String ownerFirstName;
    private String ownerLastName;

    private String residentFirstName;
    private String residentLastName;

    private String number;
    private String floor;
    private String sqMetersApart;

    private Boolean parkingSpace;
    private String parkingSlot;

    private Boolean rented;

    private Double commonPercent;
    private Double elevatorPercent;
    private Double heatingPercent;

    private Boolean storageExist;
    private String storageSlot;

    private String description;
}