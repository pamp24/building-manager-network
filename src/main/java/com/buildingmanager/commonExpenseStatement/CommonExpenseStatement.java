package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.building.Building;
import com.buildingmanager.common.BaseEntity;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseItem.CommonExpenseItem;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CommonExpenseStatement extends BaseEntity {

    private String code; // μοναδικός κωδικός
    private String type; // Νέο, Συμπληρωματικό
    private String month; // π.χ. 2025-09

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime dueDate;    // ημερομηνία λήξης πληρωμής

    private Double discountPercent;
    private Double taxPercent;
    private Double subTotal;
    private Double total;
    private Boolean isPaid = false;

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
