package com.buildingmanager.apartment;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApartmentDTO {
    private Long id;

    private String fullName;
    private String number;
    private String sqMetersApart;
    private String floor;

    private Boolean parkingSpace;
    private Boolean isRented;
    private String tenantFullName;
    private String parkingSlot;

    private Boolean apStorageExist;
    private String storageSlot;
    private Boolean isManagerHouse;

    private Double commonPercent;
    private Double elevatorPercent;
    private Double heatingPercent;

    private Boolean active;
    private Boolean enable;

    private String apDescription;

    private Long buildingId;

    private String resident;
    private String owner;

    private String ownerEmail;
    private String ownerPhone;
    private String ownerCountry;
    private String ownerFullName;

}

