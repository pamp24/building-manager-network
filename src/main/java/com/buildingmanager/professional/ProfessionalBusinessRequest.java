package com.buildingmanager.professional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfessionalBusinessRequest {

    @NotBlank
    private String businessName;

    @NotBlank
    private String ownerFullName;

    @NotNull
    private ProfessionalCategory category;

    private String description;

    @NotBlank
    private String phone;

    @Email
    private String email;

    private String website;
    private String country;

    @NotBlank
    private String city;

    private String region;

    private String address;

    private String taxNumber;
}