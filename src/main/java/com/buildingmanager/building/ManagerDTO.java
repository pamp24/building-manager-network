package com.buildingmanager.building;


public record ManagerDTO(
        Integer id,
        String fullName,
        String email,
        String phoneNumber,
        String street1,
        String stNumber1,
        String street2,
        String stNumber2,
        String profileImgUrl

) {}
