package com.buildingmanager.userDashboard;


import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;

import java.math.BigDecimal;

public class UserStatementMapper {

        public static UserStatementDTO toDTO(
                CommonExpenseStatement s,
                BigDecimal totalForApartment,
                BigDecimal paidAmount,
                BigDecimal remaining
        ) {
            return UserStatementDTO.builder()
                    .statementId(s.getId())
                    .month(s.getMonth())
                    .code(s.getCode())

                    .totalForBuilding(
                            s.getTotal() != null ? s.getTotal().doubleValue() : 0.0
                    )
                    .totalForApartment(
                            totalForApartment != null ? totalForApartment.doubleValue() : 0.0
                    )
                    .paidAmount(
                            paidAmount != null ? paidAmount.doubleValue() : 0.0
                    )
                    .remainingAmount(
                            remaining != null ? remaining.doubleValue() : 0.0
                    )

                    .issueDate(s.getStartDate().toLocalDate())
                    .dueDate(s.getEndDate() != null ? s.getEndDate().toLocalDate() : null)
                    .status(s.getStatus().name())
                    .build();
        }
    }

