package com.buildingmanager.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatementUserPaymentDTO {
    private Integer userId;
    private String userFullName;
    private Integer apartmentId;
    private String apartmentNumber;
    private String apartmentFloor;
    private Double amountToPay;
    private Double paidAmount;
    private LocalDateTime paidDate;
    private PaymentMethod paymentMethod;
    private String status;
}
