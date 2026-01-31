package com.buildingmanager.commonExpenseAllocation;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.common.BaseEntity;
import com.buildingmanager.commonExpenseItem.CommonExpenseItem;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.payment.PaymentMethod;
import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CommonExpenseAllocation extends BaseEntity {

    @ManyToOne(optional = false)
    private CommonExpenseStatement statement;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private CommonExpenseItem item;

    @ManyToOne(optional = false)
    private Apartment apartment;

    // snapshot χιλιοστών (ιστορικότητα)
    private Double commonPercent;
    private Double elevatorPercent;
    private Double heatingPercent;

    // ποσό που χρεώθηκε
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(precision = 12, scale = 2)
    private BigDecimal paidAmount;

    // κατάσταση πληρωμής
    @Column(nullable = false)
    private Boolean isPaid = false;

    private LocalDateTime paidDate;       // πότε πληρώθηκε
    private LocalDateTime dueDate;        // προθεσμία πληρωμής
    private String paymentReference;      // τραπεζικό ref / αποδεικτικό
    private String status; // PENDING, PAID, OVERDUE

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;


    private boolean active = true;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}

