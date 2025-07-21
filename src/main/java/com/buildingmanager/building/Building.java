package com.buildingmanager.building;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.bill.Bill;
import com.buildingmanager.common.BaseEntity;
import com.buildingmanager.company.Company;
import com.buildingmanager.parking.Parking;
import com.buildingmanager.user.User;
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
    private String sqMetersTotal;
    private String sqMetersCommonSpaces;
    private boolean parkingExists;
    private Integer parkingSpacesNum;
    private String buildingDescription;
    private String documentPath;
    private boolean active;
    private boolean enable;

    @ManyToMany
    @JoinTable(
            name = "user_building",
            joinColumns = @JoinColumn(name = "building_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> users;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    @OneToMany(mappedBy = "building")
    private List<Apartment> apartments;

    @OneToMany(mappedBy = "building")
    private List<Parking> parking;

    @OneToMany(mappedBy = "building")
    private List<Bill> bills;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

}


