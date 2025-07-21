package com.buildingmanager.apartment;


import com.buildingmanager.bill.ApartmentBillShare;
import com.buildingmanager.building.Building;
import com.buildingmanager.common.BaseEntity;
import com.buildingmanager.parking.Parking;
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

    private String fullName; // Ιδιοκτήτης
    private String number;
    private String sqMetersApart;
    private Integer floor;
    private boolean parkingSpace;

    private Boolean isRented;
    private String tenantFullName;

    private String parkingSlot;

    private Double commonPercent;
    private Double elevatorPercent;
    private Double heatingPercent;

    private boolean active;
    private boolean enable;

    @ManyToOne
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @OneToMany
    @JoinColumn(name = "apartment_id")
    private List<Parking> parking;

    @OneToMany(mappedBy = "apartment")
    private List<ApartmentBillShare> sharedBills;

    public String fullApartmentName() {
        return number + " " + floor;
    }
}
