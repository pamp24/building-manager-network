package com.buildingmanager.payment;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PaymentDTO {
    private Integer id;
    private String userFullName;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private Integer userId;
    private Integer statementId;
    private PaymentGateway gateway;
    private String gatewayTransactionId;
    private String gatewayStatus;

    public PaymentDTO(Integer id, String userFullName, BigDecimal amount, LocalDateTime paymentDate,
                      PaymentMethod paymentMethod, String referenceNumber, Integer userId) {
        this.id = id;
        this.userFullName = userFullName;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.userId = userId;
    }

    public PaymentDTO(Integer id, String userFullName, BigDecimal amount, LocalDateTime paymentDate,
                      PaymentMethod paymentMethod, String referenceNumber, Integer userId, Integer statementId) {
        this.id = id;
        this.userFullName = userFullName;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.userId = userId;
        this.statementId = statementId;
    }
}
