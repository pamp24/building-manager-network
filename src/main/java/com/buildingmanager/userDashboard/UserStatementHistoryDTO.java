package com.buildingmanager.userDashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatementHistoryDTO {

    private Integer statementId;
    private String month;
    private Double total;
}
