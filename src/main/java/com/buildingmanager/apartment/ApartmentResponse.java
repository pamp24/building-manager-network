package com.buildingmanager.apartment;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApartmentResponse {

    private String fullName;
    private String tenantFullName;
    private boolean isRented;
    private String number;
    private String sqMetersApart;
    private String floor;
    private boolean parkingSpace;
    private String parkingSlot;
    private boolean active;
    private boolean enable;
    private String buildingName;
    private String fullApartmentName;

    private double commonPercent;
    private double elevatorPercent;
    private double heatingPercent;

    private boolean apStorageExist;
    private String storageSlot;
    private boolean isManagerHouse;

    private String apDescription;

    private String managerFullName;
    private String managerId;

    private String resident;
    private String owner;

    private String ownerFullName;
    private String ownerEmail;
    private String ownerPhone;
    private String ownerCity;
    private String ownerStreet;
    private String ownerStreetNumber;



}
