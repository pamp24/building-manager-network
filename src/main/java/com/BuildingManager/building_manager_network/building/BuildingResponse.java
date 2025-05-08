package com.BuildingManager.building_manager_network.building;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BuildingResponse {

    private Integer id;
    private String name;
    private String street;
    private String stNumber;
    private String city;
    private String region;
    private String postalCode;
    private String country;
    private String floors;
    private int apartmentsNum;
    private String sqMetersTotal;
    private String sqMetersCommonSpaces;
    private boolean parkingExists;
    private int parkingSpacesNum;
    private boolean active;
    private boolean enable;

    private String User;

}
