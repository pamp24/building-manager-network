package com.buildingmanager.building;


import com.buildingmanager.company.CompanyDTO;
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
    private Integer floors;
    private int apartmentsNum;
    private double sqMetersTotal;
    private double sqMetersCommonSpaces;
    private boolean parkingExist;
    private int parkingSpacesNum;
    private String buildingDescription;
    private boolean hasCentralHeating;
    private String heatingType;
    private Double heatingCapacityLitres;

    private boolean active;
    private boolean enable;

    private boolean undergroundFloorExist;
    private boolean halfFloorExist;
    private boolean overTopFloorExist;
    private boolean managerHouseExist;
    private boolean storageExist;
    private Integer storageNum;

    private String managerId;
    private String managerFullName;
    private String managerEmail;
    private String managerRole;
    private String managerPhone;
    private String managerAddress1;
    private String managerCity;
    private String managerProfileImgUrl;

    private CompanyDTO company;


}
