package com.buildingmanager.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentIntentRequest {
    @NotNull
    private Integer statementId;
    @NotNull
    private Integer userId;
    @NotNull
    private Integer apartmentId;
    @NotNull
    private Double amount;
    private String returnUrl;
    private String cancelUrl;
    @NotBlank
    private String gateway;
}
