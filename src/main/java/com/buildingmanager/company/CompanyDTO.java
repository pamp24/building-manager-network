package com.buildingmanager.company;

import lombok.Data;

@Data
public class CompanyDTO {
    private Long id;
    private String companyName;
    private String taxId;
    private String managerName;
    private String email;
    private String phone;
    private String address;
    private String addressNumber;
    private String postalCode;
    private String city;
    private String region;
    private String country;
}