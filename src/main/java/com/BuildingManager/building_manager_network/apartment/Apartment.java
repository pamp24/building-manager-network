package com.BuildingManager.building_manager_network.apartment;


import com.BuildingManager.building_manager_network.bill.ApartmentBillShare;
import com.BuildingManager.building_manager_network.building.Building;
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
@Table(name = "apartment")
public class Apartment extends BaseEntity {

    private String name;
    private String number;
    private String sqMetersApart;
    private int floor;
    private boolean parkingSpace;
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


}
