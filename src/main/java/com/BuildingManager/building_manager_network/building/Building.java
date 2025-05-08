package com.BuildingManager.building_manager_network.building;

import com.BuildingManager.building_manager_network.apartment.Apartment;
import com.BuildingManager.building_manager_network.bill.Bill;
import com.BuildingManager.building_manager_network.common.BaseEntity;
import com.BuildingManager.building_manager_network.parking.Parking;
import com.BuildingManager.building_manager_network.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "building")
public class Building extends BaseEntity {

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

    @ManyToMany
    @JoinTable(
            name = "user_building",
            joinColumns = @JoinColumn(name = "building_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> users;

    @OneToMany(mappedBy = "building")
    private List<Apartment> apartments;

    @OneToMany(mappedBy = "building")
    private List<Parking> parking;

    @OneToMany(mappedBy = "building")
    private List<Bill> bills;

}


