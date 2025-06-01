package com.buildingmanager.apartment;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record ApartmentRequest(

        @NotBlank(message = "200")
        String name,
        @NotBlank(message = "201")
        String number,
        @NotBlank(message = "202")
        String sqMetersApart,
        @NotNull(message = "203")
        int floor,
        @NotNull(message = "204")
        Boolean parkingSpace,
        @NotNull(message = "205")
        Boolean active,
        @NotNull(message = "206")
        Boolean enable,
        @NotNull(message = "207")
        Integer buildingId
) {}
