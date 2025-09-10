package com.buildingmanager.apartment;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record ApartmentRequest(
        Integer id,
        @NotBlank String ownerFirstName,
        @NotBlank String ownerLastName,
        @NotBlank String number,
        @NotNull String floor,
        @NotNull String sqMetersApart,
        @NotNull Double commonPercent,
        @NotNull Double elevatorPercent,
        @NotNull Double heatingPercent,
        @NotNull Boolean isRented,
        String residentFirstName,
        String residentLastName,
        @NotNull Boolean parkingSpace,
        String parkingSlot,
        boolean apStorageExist,
        String storageSlot,
        boolean isManagerHouse,
        String lastModifiedDate,

        String apDescription,

        @NotNull Boolean active,
        @NotNull Boolean enable,
        @NotNull Integer buildingId,

        Integer residentId,
        Integer ownerId
) {}



