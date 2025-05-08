package com.BuildingManager.building_manager_network.parking;

import com.BuildingManager.building_manager_network.apartment.Apartment;
import com.BuildingManager.building_manager_network.building.Building;
import com.BuildingManager.building_manager_network.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


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


