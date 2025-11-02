package com.buildingmanager.payment;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.common.BaseEntity;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "payment")
public class Payment extends BaseEntity {

    private Double amount;

    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod; // 'CASH','BANK_TRANSFER','CARD',...

    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id")
    private CommonExpenseStatement statement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;
}
