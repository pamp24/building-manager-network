package com.BuildingManager.building_manager_network.bill;

import com.BuildingManager.building_manager_network.apartment.Apartment;
import com.BuildingManager.building_manager_network.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;


@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "apartmentBillShare")
public class ApartmentBillShare extends BaseEntity {


    @ManyToOne
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;


    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    private BigDecimal shareAmount;
    private boolean paid;

}

