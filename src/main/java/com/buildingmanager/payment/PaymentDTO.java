package com.buildingmanager.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDTO {
    private Integer id;
    private String userFullName;
    private Double amount;
    private LocalDateTime paymentDate;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private Integer userId;
    private Integer statementId;
}
