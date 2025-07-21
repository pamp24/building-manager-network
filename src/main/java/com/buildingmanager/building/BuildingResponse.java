package com.buildingmanager.building;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BuildingResponse {

    private String buildingCode;
    private Integer id;
    private String name;
    private String street1;
    private String stNumber1;
    private String street2;
    private String stNumber2;
    private String city;
    private String region;
    private String postalCode;
    private String country;
    private String state;
    private String floors;
    private int apartmentsNum;
    private String sqMetersTotal;
    private String sqMetersCommonSpaces;
    private boolean parkingExists;
    private int parkingSpacesNum;
    private String buildingDescription;
    private boolean active;
    private boolean enable;

    private String managerId;
    private String managerFullName;
    private String managerEmail;
    private String managerPhone;
    private String managerAddress1;
    private String managerCity;

}
