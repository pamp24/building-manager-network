package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.building.Building;
import com.buildingmanager.common.BaseEntity;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseItem.CommonExpenseItem;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CommonExpenseStatement extends BaseEntity {

    private String code;
    private String type;
    private String month;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Double discountPercent;
    private Double taxPercent;
    private Double subTotal;
    private Double total;
    private Boolean isPaid = false;

    @Transient
    private boolean hasPayments;

    private String description;

    @Enumerated(EnumType.STRING)
    private StatementStatus status;

    private Integer sequenceNumber;

    @ManyToOne
    private Building building;

    @OneToMany(mappedBy = "statement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommonExpenseItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "statement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommonExpenseAllocation> allocations = new ArrayList<>();

    private boolean active = true;
}
