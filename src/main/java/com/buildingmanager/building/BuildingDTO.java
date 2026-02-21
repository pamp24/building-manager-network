package com.buildingmanager.building;


import com.buildingmanager.company.CompanyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BuildingDTO {
    private Integer id;
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
    private boolean parkingExist;
    private Integer parkingSpacesNum;
    private String buildingDescription;

    private boolean hasCentralHeating;
    private String heatingType;
    private Double heatingCapacityLitres;

    private String buildingCode;

    private boolean undergroundFloorExist;
    private boolean halfFloorExist;
    private boolean overTopFloorExist;
    private boolean managerHouseExist;
    private boolean storageExist;
    private Integer storageNum;

    private String managerFullName;
    private String managerEmail;
    private String managerPhone;
    private String managerAddress1;
    private String managerCity;
    private String managerProfileImgUrl;
    private String managerRole;

    private CompanyDTO company;
}


