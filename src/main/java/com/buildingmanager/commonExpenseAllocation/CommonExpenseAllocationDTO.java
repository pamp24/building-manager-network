package com.buildingmanager.commonExpenseAllocation;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CommonExpenseAllocationDTO {

    private Integer id;
    private String itemDescription;
    private Double amount;
    private boolean isPaid;
    private LocalDateTime paidDate;

    private Integer apartmentId;
}
