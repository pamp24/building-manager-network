package com.BuildingManager.building_manager_network.building;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BuildingRequest(
        Integer id,
        @NotNull(message = "100")
        @NotEmpty(message = "100")
        String name,
        @NotNull(message = "101")
        @NotEmpty(message = "101")
        String street,
        @NotNull(message = "102")
        @NotEmpty(message = "102")
        String stNumber,
        @NotNull(message = "103")
        @NotEmpty(message = "103")
        String city,
        @NotNull(message = "104")
        @NotEmpty(message = "104")
        String region,
        @NotNull(message = "105")
        @NotEmpty(message = "105")
        String postalCode,
        @NotNull(message = "106")
        @NotEmpty(message = "106")
        String country,
        @NotNull(message = "107")
        @NotEmpty(message = "107")
        String floors,
        @NotNull(message = "108")
        @NotEmpty(message = "108")
        int apartmentsNum,
        @NotNull(message = "109")
        @NotEmpty(message = "109")
        String sqMetersTotal,
        @NotNull(message = "110")
        @NotEmpty(message = "110")
        String sqMetersCommonSpaces,
        @NotNull(message = "111")
        @NotEmpty(message = "111")
        boolean parkingExists,
        @NotNull(message = "112")
        @NotEmpty(message = "112")
        int parkingSpacesNum,
        @NotNull(message = "113")
        @NotEmpty(message = "113")
        boolean active,
        @NotNull(message = "114")
        @NotEmpty(message = "114")
        boolean enable

) {
}
