package com.buildingmanager.userDashboard;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatementHistoryDTO {

    private Integer statementId;
    private String month;
    private BigDecimal billed;
    private BigDecimal remaining;
}
