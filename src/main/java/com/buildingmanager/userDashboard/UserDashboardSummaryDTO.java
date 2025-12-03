package com.buildingmanager.userDashboard;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDashboardSummaryDTO {

    private BigDecimal latestDebt;     // Τελευταίο υπόλοιπο που χρωστάει ο χρήστης
    private Integer statementId;       // Από ποιο statement προέρχεται
    private String statementMonth;     // Μήνας statement
    private String role;               // Owner / Resident
}
