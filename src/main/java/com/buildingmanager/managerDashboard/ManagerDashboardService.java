package com.buildingmanager.managerDashboard;

import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerDashboardService {

    private final CommonExpenseStatementRepository commonExpenseStatementRepository;
    private final CommonExpenseAllocationRepository commonExpenseAllocationRepository;

    public ManagerDashboardDTO getDashboardForBuilding(Integer buildingId) {
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        long totalIssued = commonExpenseStatementRepository
                .countByBuildingAndMonthAndYear(buildingId, currentMonth, currentYear);

        long totalPaid = commonExpenseStatementRepository
                .countByBuildingAndMonthAndYear(
                        buildingId, currentMonth, currentYear);

        long totalPending = commonExpenseStatementRepository
                .countUnpaidByBuildingAndMonthAndYear(
                        buildingId, currentMonth, currentYear);

        long totalOverdue = commonExpenseStatementRepository
                .countOverdueByBuilding(buildingId, now);

        Double totalIncome = commonExpenseStatementRepository.sumPaidAmountByBuilding(buildingId);
        Double totalDebt = commonExpenseStatementRepository.sumUnpaidAmountByBuilding(buildingId);

        // ΝΕΟ — Chart data (Issued vs Paid per month)
        List<MonthlyStatsDTO> monthlyStats = getIssuedVsPaidPerMonth(buildingId);

//        System.out.println("Checking buildingId=" + buildingId + ", month=" + currentMonth + ", year=" + currentYear);
//        System.out.println("Total issued query result = " + totalIssued);

        return ManagerDashboardDTO.builder()
                .buildingId(buildingId)
                .totalIssued(totalIssued)
                .totalPaid(totalPaid)
                .totalPending(totalPending)
                .totalOverdue(totalOverdue)
                .totalIncome(totalIncome != null ? totalIncome : 0.0)
                .totalDebt(totalDebt != null ? totalDebt : 0.0)
                .monthlyStats(monthlyStats)
                .build();
    }


    public List<MonthlyStatsDTO> getIssuedVsPaidPerMonth(Integer buildingId) {
        int currentYear = LocalDate.now().getYear(); // Χρησιμοποίησε το σωστό έτος
        List<MonthlyStatsDTO> stats = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            long issued = commonExpenseStatementRepository
                    .countByBuildingAndMonthAndYear(buildingId, month, currentYear);
            long paid = commonExpenseStatementRepository
                    .countPaidByBuildingAndMonthAndYear(buildingId, month, currentYear);

            stats.add(new MonthlyStatsDTO(getMonthName(month), issued, paid));
        }

        return stats;
    }


    private String getMonthName(int month) {
        return switch (month) {
            case 1 -> "Ιαν";
            case 2 -> "Φεβ";
            case 3 -> "Μαρ";
            case 4 -> "Απρ";
            case 5 -> "Μαι";
            case 6 -> "Ιουν";
            case 7 -> "Ιουλ";
            case 8 -> "Αυγ";
            case 9 -> "Σεπ";
            case 10 -> "Οκτ";
            case 11 -> "Νοε";
            case 12 -> "Δεκ";
            default -> "";
        };
    }
}

