package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationDTO;
import com.buildingmanager.commonExpenseItem.CommonExpenseItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonExpenseStatementDTO {

    private Integer id;
    private String code;
    private String type;
    private String month;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Double discountPercent;
    private Double taxPercent;
    private Double subTotal;
    private Double total;

    private String description;

    private Integer sequenceNumber;
    private Integer buildingId;
    private StatementStatus status;
    private Boolean isPaid;

    private List<CommonExpenseItemDTO> items;
    private List<CommonExpenseAllocationDTO> allocations;

}
