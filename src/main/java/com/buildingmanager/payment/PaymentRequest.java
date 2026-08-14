package com.buildingmanager.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class PaymentRequest {
    @NotNull
    private Integer statementId;
    private Integer userId;
    private Integer apartmentId;
    @NotNull
    private Double amount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String referenceNumber;
    private String gateway;
    private String gatewayPaymentMethodId;
    private String returnUrl;
    private String cancelUrl;
}
