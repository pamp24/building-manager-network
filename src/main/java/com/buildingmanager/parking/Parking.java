package com.buildingmanager.parking;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.building.Building;
import com.buildingmanager.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Parking extends BaseEntity {

    private String name;
    private int number;
    private boolean active;
    private boolean enable;

    @ManyToOne(optional = true)
    @JoinColumn(name = "apartment_id")
    private Apartment apartments;

    @ManyToOne(optional = false)
    @JoinColumn(name = "building_id")
    private Building building;

}


