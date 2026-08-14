package com.buildingmanager.finance;

import com.buildingmanager.commonExpenseItem.ExpenseCategorySummaryDTO;
import com.buildingmanager.payment.PaymentDTO;
import com.buildingmanager.userDashboard.UserStatementDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingFinanceDTO {
    private String buildingName;
    private String buildingCode;
    private String userRole;
    private boolean managerView;

    private BigDecimal myPending;
    private BigDecimal myPaid;
    private Integer lastStatementId;
    private String lastStatementMonth;

    private List<UserStatementDTO> statements;
    private List<PaymentDTO> recentPayments;
    private List<ExpenseCategorySummaryDTO> currentMonthExpenses;

    private BigDecimal totalBuildingPending;
    private BigDecimal totalBuildingCollected;
    private Double collectionRate;
    private Integer totalApartments;
    private Integer totalMembers;
    private List<MemberPaymentStatus> memberPaymentStatuses;
}
