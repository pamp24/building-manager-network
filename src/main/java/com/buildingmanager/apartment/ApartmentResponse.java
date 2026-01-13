package com.buildingmanager.apartment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApartmentResponse {

    private Integer id;
    private String ownerFirstName;
    private String ownerLastName;
    private String residentFirstName;
    private String residentLastName;
    @JsonProperty("isRented")
    private boolean isRented;
    private String number;
    private String sqMetersApart;
    private String floor;
    private boolean parkingSpace;
    private String parkingSlot;
    private boolean active;
    private boolean enable;
    private LocalDateTime lastModifiedDate;

    private Integer buildingId;
    private String buildingName;
    private String buildingStreet;
    private String buildingStreetNumber;
    private String buildingCity;

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
    private Integer residentId;
    private String residentFullName;
    private String residentEmail;
    private String residentPhone;

    private String owner;
    private Integer ownerId;
    private String ownerFullName;
    private String ownerEmail;
    private String ownerPhone;
    private String ownerCity;
    private String ownerStreet;
    private String ownerStreetNumber;




}
