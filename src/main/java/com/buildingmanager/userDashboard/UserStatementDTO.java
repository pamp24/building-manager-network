package com.buildingmanager.userDashboard;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserStatementDTO {

    private Integer statementId;
    private String month;
    private String code;
    private Double totalForBuilding;   // σύνολο statement
    private Double totalForApartment;  // πόσο οφείλει ο user (owner ή resident)
    private Double paidAmount;         // πόσο έχει πληρώσει
    private Double remainingAmount;    // totalForApartment - paidAmount
    private Boolean isPaid;
    private LocalDate issueDate;
    private LocalDate dueDate;

    private String status;
}

