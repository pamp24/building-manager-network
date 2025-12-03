package com.buildingmanager.userDashboard;


import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;

public class UserStatementMapper {

    public static UserStatementDTO toDTO(
            CommonExpenseStatement s,
            double totalForApartment,
            double paidAmount,
            double remaining
    ) {
        return UserStatementDTO.builder()
                .statementId(s.getId())
                .month(s.getMonth())
                .totalForBuilding(s.getTotal())
                .totalForApartment(totalForApartment)
                .paidAmount(paidAmount)
                .remainingAmount(remaining)
                .code(s.getCode())
                .issueDate(s.getStartDate().toLocalDate())
                .dueDate(s.getEndDate() != null ? s.getEndDate().toLocalDate() : null)
                .status(s.getStatus().name())
                .build();
    }
}
