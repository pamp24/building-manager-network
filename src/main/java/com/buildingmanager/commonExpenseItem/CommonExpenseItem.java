package com.buildingmanager.commonExpenseItem;

import com.buildingmanager.common.BaseEntity;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CommonExpenseItem extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private ExpenseCategory category; // COMMON, HEATING, ELEVATOR, EQUAL

    private String descriptionItem; // περιγραφή μικρή

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "statement_id")
    private CommonExpenseStatement statement;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommonExpenseAllocation> allocations = new ArrayList<>();

    private boolean active = true;
}
