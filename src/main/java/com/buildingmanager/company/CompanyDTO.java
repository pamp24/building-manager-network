package com.buildingmanager.company;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyDTO {
    private Integer companyId;
    private String companyName;
    private String taxNumber;
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