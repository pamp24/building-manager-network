package com.buildingmanager.apartment;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record ApartmentRequest(
        @NotBlank String fullName,
        @NotBlank String number,
        @NotNull Integer floor,
        @NotNull String sqMetersApart,
        @NotNull Double commonPercent,
        @NotNull Double elevatorPercent,
        @NotNull Double heatingPercent,
        @NotNull Boolean isRented,
        String tenantFullName,
        @NotNull Boolean parkingSpace,
        String parkingSlot,
        @NotNull Boolean active,
        @NotNull Boolean enable,
        @NotNull Integer buildingId
) {}



