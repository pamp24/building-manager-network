package com.buildingmanager.managerDashboard;

import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import com.buildingmanager.commonExpenseStatement.StatementStatus;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerDashboardService {

    private final CommonExpenseStatementRepository commonExpenseStatementRepository;
    private final CommonExpenseAllocationRepository commonExpenseAllocationRepository;

    public ManagerDashboardDTO getDashboardForBuilding(Integer buildingId) {
        int currentYear = LocalDate.now().getYear();

        //Μετράμε ΟΛΑ τα εκδοθέντα του έτους (όχι μόνο ενός μήνα)
        long totalIssued = commonExpenseStatementRepository
                .countIssuedExcludingDraftsAndCancelledForYear(buildingId, currentYear);

        long totalPaid = commonExpenseStatementRepository.countByBuildingAndStatus(buildingId, StatementStatus.PAID);
        long totalExpired = commonExpenseStatementRepository.countByBuildingAndStatus(buildingId, StatementStatus.EXPIRED);
        long totalDraft = commonExpenseStatementRepository.countByBuildingAndStatus(buildingId, StatementStatus.DRAFT);
        long totalCancelled = commonExpenseStatementRepository.countByBuildingAndStatus(buildingId, StatementStatus.CLOSED);

        //Εκκρεμή = Εκδοθέντα - (Πληρωμένα + Ληγμένα)
        long totalPending = totalIssued - (totalPaid + totalExpired);
        if (totalPending < 0) totalPending = 0;

        Double totalIncome = commonExpenseStatementRepository.sumPaidAmountByBuilding(buildingId);
        Double totalDebt = commonExpenseStatementRepository.sumUnpaidAmountByBuilding(buildingId);

        List<MonthlyStatsDTO> monthlyStats = getIssuedVsPaidPerMonth(buildingId);
        List<MonthlyAmountStatsDTO> monthlyAmountStats = getMonthlyAmountStats(buildingId);

        return ManagerDashboardDTO.builder()
                .buildingId(buildingId)
                .totalIssued(totalIssued)
                .totalPaid(totalPaid)
                .totalPending(totalPending)
                .totalExpired(totalExpired)
                .totalCancelled(totalCancelled)
                .totalDraft(totalDraft)
                .totalIncome(totalIncome != null ? totalIncome : 0.0)
                .totalDebt(totalDebt != null ? totalDebt : 0.0)
                .monthlyStats(monthlyStats)
                .monthlyAmountStats(monthlyAmountStats)
                .build();
    }

    public List<MonthlyStatsDTO> getIssuedVsPaidPerMonth(Integer buildingId) {
        int currentYear = LocalDate.now().getYear();
        List<MonthlyStatsDTO> stats = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            long issued = commonExpenseStatementRepository.countIssuedExcludingDraftsAndCancelled(buildingId, month, currentYear);
            long paid = commonExpenseStatementRepository.countByBuildingMonthYearAndStatus(buildingId, month, currentYear, StatementStatus.PAID);
            long expired = commonExpenseStatementRepository.countByBuildingMonthYearAndStatus(buildingId, month, currentYear, StatementStatus.EXPIRED);

            //Εκκρεμή = Εκδοθέντα - (Πληρωμένα + Ληγμένα)
            long pending = issued - (paid + expired);
            if (pending < 0) pending = 0;

            stats.add(new MonthlyStatsDTO(getMonthName(month), issued, pending, paid, expired));
        }

        return stats;
    }
    public List<MonthlyAmountStatsDTO> getMonthlyAmountStats(Integer buildingId) {
        int currentYear = LocalDate.now().getYear();
        List<MonthlyAmountStatsDTO> stats = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            Double issued = commonExpenseStatementRepository.sumByBuildingMonthYearAndStatuses(
                    buildingId, month, currentYear,
                    List.of(StatementStatus.ISSUED, StatementStatus.PAID, StatementStatus.EXPIRED));

            Double paid = commonExpenseStatementRepository.sumByBuildingMonthYearAndStatuses(
                    buildingId, month, currentYear, List.of(StatementStatus.PAID));

            Double expired = commonExpenseStatementRepository.sumByBuildingMonthYearAndStatuses(
                    buildingId, month, currentYear, List.of(StatementStatus.EXPIRED));

            Double pending = (issued != null ? issued : 0.0) - ((paid != null ? paid : 0.0) + (expired != null ? expired : 0.0));
            if (pending < 0) pending = 0.0;
            stats.add(new MonthlyAmountStatsDTO(
                    getMonthName(month),
                    issued != null ? issued : 0.0,
                    pending,
                    paid != null ? paid : 0.0,
                    expired != null ? expired : 0.0
            ));
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
