package com.buildingmanager.building;

public record ManagedBuildingDTO(
        Integer id,
        String name,
        String street1,
        String stNumber1,
        String street2,
        String stNumber2,
        String city,
        String postalCode,
        ManagerDTO manager
) {}
