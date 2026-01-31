package com.buildingmanager.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatementUserPaymentDTO {
    private Integer userId;
    private String userFirstName;
    private String userLastName;
    private Integer apartmentId;
    private String apartmentNumber;
    private String apartmentFloor;
    private BigDecimal amountToPay;
    private BigDecimal paidAmount;
    private LocalDateTime paidDate;
    private PaymentMethod paymentMethod;
    private String status;
}
