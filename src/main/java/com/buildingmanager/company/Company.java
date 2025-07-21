package com.buildingmanager.company;

import com.buildingmanager.building.Building;
import com.buildingmanager.common.BaseEntity;
import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Company extends BaseEntity {


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

    @OneToOne(mappedBy = "company")
    private User propertyManager;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Building> buildings;
}
