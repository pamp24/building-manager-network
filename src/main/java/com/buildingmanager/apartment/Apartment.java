package com.buildingmanager.apartment;


import com.buildingmanager.bill.ApartmentBillShare;
import com.buildingmanager.building.Building;
import com.buildingmanager.common.BaseEntity;
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
@Table(name = "apartment")
public class Apartment extends BaseEntity {

    private String fullName;
    private String number;
    private String sqMetersApart;
    private String floor;
    private boolean parkingSpace;

    private Boolean isRented;
    private String tenantFullName;

    private String parkingSlot;

    private boolean apStorageExist;
    private String storageSlot;

    private boolean isManagerHouse;

    private Double commonPercent;
    private Double elevatorPercent;
    private Double heatingPercent;

    private String apDescription;

    private boolean active;
    private boolean enable;

    @ManyToOne
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @ManyToOne
    @JoinColumn(name = "resident_id")
    private User resident;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany
    @JoinColumn(name = "parking_id")
    private List<Parking> parking;

    @OneToMany(mappedBy = "apartment")
    private List<ApartmentBillShare> sharedBills;

    public String fullApartmentName() {
        return number + " " + floor;
    }

}

