package com.buildingmanager.building;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BuildingRequest(
        Integer id,

        @NotBlank(message = "100")
        String name,
        @NotBlank(message = "101")
        String street1,
        @NotBlank(message = "102")
        String stNumber1,
        String street2,
        String stNumber2,
        @NotBlank(message = "103")
        String city,
        @NotBlank(message = "104")
        String region,
        @NotBlank(message = "105")
        String postalCode,
        @NotBlank(message = "106")
        String country,
        @NotBlank(message = "107")
        String state,
        @NotBlank(message = "108")
        String floors,
        @NotNull(message = "109")
        Integer apartmentsNum,
        @NotBlank(message = "110")
        String sqMetersTotal,
        @NotBlank(message = "111")
        String sqMetersCommonSpaces,
        @NotNull(message = "112")
        Boolean parkingExists,
        @NotNull(message = "113")
        Integer parkingSpacesNum,
        @NotNull(message = "114")
        Boolean active,
        @NotNull(message = "115")
        Boolean enable,
        String buildingDescription
) {}

