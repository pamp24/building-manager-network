package com.buildingmanager.building;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BuildingRequest(
        Integer id,

        @NotBlank(message = "100")
        String name,

        @NotBlank(message = "101")
        String street,

        @NotBlank(message = "102")
        String stNumber,

        @NotBlank(message = "103")
        String city,

        @NotBlank(message = "104")
        String region,

        @NotBlank(message = "105")
        String postalCode,

        @NotBlank(message = "106")
        String country,

        @NotBlank(message = "107")
        String floors,

        @NotNull(message = "108")
        Integer apartmentsNum,

        @NotBlank(message = "109")
        String sqMetersTotal,

        @NotBlank(message = "110")
        String sqMetersCommonSpaces,

        @NotNull(message = "111")
        Boolean parkingExists,

        @NotNull(message = "112")
        Integer parkingSpacesNum,

        @NotNull(message = "113")
        Boolean active,

        @NotNull(message = "114")
        Boolean enable
) {}

