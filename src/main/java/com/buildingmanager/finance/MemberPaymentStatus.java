package com.buildingmanager.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPaymentStatus {
    private Integer userId;
    private String fullName;
    private Integer apartmentId;
    private String apartmentNumber;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private String status;
}
