package com.buildingmanager.building;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuildingDTO {

    private String name;
    private String street1;
    private String stNumber1;
    private String street2;
    private String stNumber2;
    private String city;
    private String state;
    private String region;
    private String postalCode;
    private String country;
    private Integer floors;
    private Integer apartmentsNum;
    private double sqMetersTotal;
    private double sqMetersCommonSpaces;
    private boolean parkingExists;
    private String parkingSpacesNum;
    private String buildingDescription;
    private String buildingCode;
    private String description;
    private boolean undergroundFloorExists;
    private boolean halfFloorExists;
    private boolean overTopFloorExists;
    private boolean managerHouseExists;
    private boolean storageExists;
    private Integer storageNum;
}

