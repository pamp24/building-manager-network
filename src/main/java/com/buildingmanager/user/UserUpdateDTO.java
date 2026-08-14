package com.buildingmanager.user;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateDTO {

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;

    private String phoneNumber;
    private String profileImageUrl;

    private String address1;
    private String addressNumber1;
    private String address2;
    private String addressNumber2;
    private String country;
    private String state;
    private String city;
    private String region;
    private String postalCode;
}

