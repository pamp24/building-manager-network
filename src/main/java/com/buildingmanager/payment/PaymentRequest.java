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
    @NotNull
    private Double amount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime paymentDate; // αν κενό, θα βάλουμε now()
    private String paymentMethod;
    private String referenceNumber;
}
