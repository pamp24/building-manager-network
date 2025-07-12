package com.buildingmanager.user;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter

public class UserResponseDTO {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
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
    private LocalDate dateOfBirth;
    private List<String> roles;

    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.profileImageUrl = user.getProfileImageUrl();
        this.address1 = user.getAddress1();
        this.addressNumber1 = user.getAddressNumber1();
        this.address2 = user.getAddress2();
        this.addressNumber2 = user.getAddressNumber2();
        this.country = user.getCountry();
        this.state = user.getState();
        this.city = user.getCity();
        this.region = user.getRegion();
        this.postalCode = user.getPostalCode();
        this.dateOfBirth = user.getDateOfBirth();
        this.roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList());
    }

}
