package com.buildingmanager.bill;

import com.buildingmanager.building.Building;
import com.buildingmanager.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;


@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bill")  // all lowercase
public class Bill extends BaseEntity {

    private String name;
    private long totalSum;
    private String active;

    @ManyToOne
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @OneToMany(mappedBy = "bill")
    private List<ApartmentBillShare> shares;


}
