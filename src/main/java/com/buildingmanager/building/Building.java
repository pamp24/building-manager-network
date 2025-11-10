package com.buildingmanager.building;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.common.BaseEntity;
import com.buildingmanager.company.Company;
import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "building")
public class Building extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String buildingCode;

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

    @Enumerated(EnumType.STRING)
    private HeatingType heatingType;

    private Double heatingCapacityLitres;
    private boolean undergroundFloorExist;
    private boolean halfFloorExist;
    private boolean overTopFloorExist;
    private boolean managerHouseExist;
    private boolean storageExist;
    private Integer storageNum;
    
    private boolean active;
    private boolean enable;

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<com.buildingmanager.building.BuildingMember> members = new ArrayList<>();



    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    @OneToMany(mappedBy = "building")
    private List<Apartment> apartments;


    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

}


