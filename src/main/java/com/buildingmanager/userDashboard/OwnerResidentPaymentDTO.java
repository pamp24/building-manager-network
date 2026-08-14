package com.buildingmanager.userDashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerResidentPaymentDTO {

    private Integer statementId;
    private String month;
    private String residentName;
    private String apartmentNumber;
    private double billed;
    private double paid;
    private double remaining;
    private boolean isPaid;
}
