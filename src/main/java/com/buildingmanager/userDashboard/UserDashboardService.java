package com.buildingmanager.userDashboard;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import com.buildingmanager.commonExpenseItem.CommonExpenseItemDTO;
import com.buildingmanager.commonExpenseItem.ExpenseCategory;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import com.buildingmanager.payment.PaymentRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDashboardService {

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final CommonExpenseStatementRepository statementRepository;
    private final CommonExpenseAllocationRepository allocationRepository;
    private final PaymentRepository paymentRepository;

    public UserDashboardSummaryDTO getDashboard(Integer userId) {

        log.info("  Loading dashboard for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));


        // 1) ΒΡΕΣ ΣΩΣΤΟ APARTMENT
        Apartment apartment = null;

        // 1) PREFFER RESIDENT
        List<Apartment> residentApts = apartmentRepository.findByResident_Id(userId);
        if (!residentApts.isEmpty()) {
            apartment = residentApts.get(0);
            log.info("Selected RESIDENT apartment {}", apartment.getId());
        }

        // 2) If no resident apartment → check owner
        if (apartment == null) {
            List<Apartment> ownerApts = apartmentRepository.findByOwner_Id(userId);
            if (!ownerApts.isEmpty()) {
                apartment = ownerApts.get(0);
                log.info("Selected OWNER apartment {}", apartment.getId());
            }
        }

        if (apartment == null) {
            log.warn("User {} has no apartment", userId);
            return new UserDashboardSummaryDTO(
                    BigDecimal.ZERO,
                    null,
                    null,
                    null
            );
        }

        Integer buildingId = apartment.getBuilding().getId();

        // 2) ΒΡΕΣ ΤΟ ΤΕΛΕΥΤΑΙΟ STATEMENT
        List<CommonExpenseStatement> statements = statementRepository.findByBuildingId(buildingId);

        if (statements.isEmpty()) {
            log.warn("No statements found for building {}", buildingId);
            return new UserDashboardSummaryDTO(BigDecimal.ZERO, null, null, null);
        }

        CommonExpenseStatement lastStatement = statements.stream()
                .max(Comparator.comparing(CommonExpenseStatement::getStartDate))
                .orElse(null);

        if (lastStatement == null) {
            return new UserDashboardSummaryDTO(BigDecimal.ZERO, null, null, null);
        }

        log.info("Last statement = {} ({})", lastStatement.getId(), lastStatement.getMonth());


        // 3) ΦΕΡΕ ΟΛΕΣ ΤΙΣ ΧΡΕΩΣΕΙΣ ΤΟΥ ΔΙΑΜΕΡΙΣΜΑΤΟΣ
        List<CommonExpenseAllocation> allocations =
                allocationRepository.findByStatementAndApartment(lastStatement, apartment);

        log.info("Found {} allocations for apartment {}", allocations.size(), apartment.getId());

        boolean isOwner = apartment.getOwner() != null &&
                apartment.getOwner().getId().equals(userId);

        boolean isResident = apartment.getResident() != null &&
                apartment.getResident().getId().equals(userId);

        String userRole = isResident ? "Resident" : "Owner";

        // 4) ΥΠΟΛΟΓΙΣΜΟΣ ΠΟΣΟΥ ΠΟΥ ΧΡΩΣΤΑΕΙ
        BigDecimal totalDue = BigDecimal.ZERO;

        for (CommonExpenseAllocation alloc : allocations) {

            BigDecimal amount = alloc.getAmount() == null ? BigDecimal.ZERO : alloc.getAmount();
            BigDecimal paid   = alloc.getPaidAmount() == null ? BigDecimal.ZERO : alloc.getPaidAmount();

            BigDecimal remaining = amount.subtract(paid);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) continue;

            ExpenseCategory category = alloc.getItem().getCategory();
            boolean hasResident = apartment.getResident() != null;

            if (hasResident) {
                if (isResident && category != ExpenseCategory.OWNERS) {
                    totalDue = totalDue.add(remaining);
                }
                if (isOwner && category == ExpenseCategory.OWNERS) {
                    totalDue = totalDue.add(remaining);
                }
                continue;
            }

            if (isOwner) {
                totalDue = totalDue.add(remaining);
            }
        }


        log.info("Final amount due: {}", totalDue);

        return UserDashboardSummaryDTO.builder()
                .latestDebt(totalDue)
                .statementId(lastStatement.getId())
                .statementMonth(lastStatement.getMonth())
                .role(userRole)
                .build();
    }

    public List<UserStatementHistoryDTO> getHistory(Integer userId) {

        log.info("Loading dashboard history for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Apartment apartment = apartmentRepository.findByResident_Id(userId).stream().findFirst()
                .orElse(apartmentRepository.findByOwner_Id(userId).stream().findFirst().orElse(null));

        if (apartment == null) {
            log.warn("User {} has no apartment -> empty history", userId);
            return List.of();
        }

        List<CommonExpenseStatement> statements =
                statementRepository.findByBuildingId(apartment.getBuilding().getId());

        statements.sort((a, b) -> b.getStartDate().compareTo(a.getStartDate()));
        statements = statements.stream().limit(12).toList();

        return statements.stream()
                .map(s -> {
                    BigDecimal billed = calculateUserBilledForStatement(s, apartment, user);
                    BigDecimal remaining = calculateUserRemainingForStatement(s, apartment, user);
                    return new UserStatementHistoryDTO(s.getId(), s.getMonth(), billed, remaining);
                })
                .toList();

    }

    private BigDecimal calculateUserBilledForStatement(CommonExpenseStatement statement, Apartment apartment, User user) {

        List<CommonExpenseAllocation> allocations =
                allocationRepository.findByStatementAndApartment(statement, apartment);

        BigDecimal total = BigDecimal.ZERO;

        boolean isOwner = apartment.getOwner() != null && apartment.getOwner().getId().equals(user.getId());
        boolean isResident = apartment.getResident() != null && apartment.getResident().getId().equals(user.getId());
        boolean hasResident = apartment.getResident() != null;

        for (CommonExpenseAllocation alloc : allocations) {

            BigDecimal amount = alloc.getAmount() == null ? BigDecimal.ZERO : alloc.getAmount();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) continue;

            ExpenseCategory category = alloc.getItem().getCategory();

            if (hasResident) {
                if (isResident && category != ExpenseCategory.OWNERS) total = total.add(amount);
                if (isOwner && category == ExpenseCategory.OWNERS) total = total.add(amount);
            } else {
                if (isOwner) total = total.add(amount);
            }
        }

        return total;
    }


    private BigDecimal calculateUserRemainingForStatement(CommonExpenseStatement statement, Apartment apartment, User user) {

        List<CommonExpenseAllocation> allocations =
                allocationRepository.findByStatementAndApartment(statement, apartment);

        BigDecimal total = BigDecimal.ZERO;

        boolean isOwner = apartment.getOwner() != null && apartment.getOwner().getId().equals(user.getId());
        boolean isResident = apartment.getResident() != null && apartment.getResident().getId().equals(user.getId());
        boolean hasResident = apartment.getResident() != null;

        for (CommonExpenseAllocation alloc : allocations) {

            BigDecimal amount = alloc.getAmount() == null ? BigDecimal.ZERO : alloc.getAmount();
            BigDecimal paid   = alloc.getPaidAmount() == null ? BigDecimal.ZERO : alloc.getPaidAmount();

            BigDecimal remaining = amount.subtract(paid);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) continue;

            ExpenseCategory category = alloc.getItem().getCategory();

            if (hasResident) {
                if (isResident && category != ExpenseCategory.OWNERS) total = total.add(remaining);
                if (isOwner && category == ExpenseCategory.OWNERS) total = total.add(remaining);
            } else {
                if (isOwner) total = total.add(remaining);
            }
        }

        return total;
    }


    public List<CommonExpenseItemDTO> getLastStatementItems(Integer userId) {

        // 1. Βρες το apartment του user
        Apartment apartment = findApartmentForUser(userId);

        if (apartment == null) return List.of();

        // 2. Πάρε το building ID
        Integer buildingId = apartment.getBuilding().getId();

        // 3. Φέρε όλα τα statements του κτιρίου
        List<CommonExpenseStatement> statements =
                statementRepository.findByBuildingId(buildingId);

        if (statements.isEmpty()) return List.of();

        // 4. Το πιο πρόσφατο statement
        CommonExpenseStatement latest = statements.stream()
                .max(Comparator.comparing(CommonExpenseStatement::getStartDate))
                .orElse(null);

        if (latest == null) return List.of();

        // 5. Πάρε τα items του statement
        return latest.getItems().stream()
                .map(item -> new CommonExpenseItemDTO(
                        item.getId(),
                        item.getCategory().name(),
                        item.getDescriptionItem(),
                        item.getPrice()
                ))
                .toList();
    }
    private Apartment findApartmentForUser(Integer userId) {

        // Βρες σαν resident
        List<Apartment> residentApts = apartmentRepository.findByResident_Id(userId);
        if (!residentApts.isEmpty()) {
            return residentApts.get(0);
        }

        // Βρες σαν owner
        List<Apartment> ownerApts = apartmentRepository.findByOwner_Id(userId);
        if (!ownerApts.isEmpty()) {
            return ownerApts.get(0);
        }

        return null;
    }

    public ChartResponseDTO getChartData(Integer userId, String type, String period) {

        Apartment apartment = findApartmentForUser(userId);
        if (apartment == null) return new ChartResponseDTO(List.of(), List.of(), BigDecimal.ZERO);


        Integer buildingId = apartment.getBuilding().getId();

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        if ("month".equalsIgnoreCase(period)) {

            //τελευταίο statement στη βάση
            LocalDateTime maxStart = statementRepository.findMaxStatementStartDate(buildingId);
            YearMonth anchor = (maxStart != null) ? YearMonth.from(maxStart) : YearMonth.now();

            YearMonth start = anchor.minusMonths(11);

            for (int i = 0; i < 12; i++) {
                YearMonth ym = start.plusMonths(i);
                int year = ym.getYear();
                int month = ym.getMonthValue();

                // label "2026-02"
                labels.add(ym.toString());

                BigDecimal amount;
                if ("building".equalsIgnoreCase(type)) {
                    amount = statementRepository.sumBuildingExpensesByMonthYear(buildingId, month, year);
                } else {
                    amount = allocationRepository.sumApartmentExpensesByMonthYear(apartment.getId(), month, year);
                }

                values.add(amount != null ? amount.doubleValue() : 0.0);
            }

        }
        // All years
        else {

            List<Integer> years = statementRepository.findAvailableYears(buildingId);
            int yearsBack = 5; // <-- άλλαξέ το σε 10 αν θες
            Integer maxYearObj = statementRepository.findMaxYearForBuilding(buildingId);
            int endYear = (maxYearObj != null) ? maxYearObj : LocalDate.now().getYear();
            int startYear = endYear - (yearsBack - 1);

            // fallback
            if (years == null || years.isEmpty()) {
                int currentYear = java.time.LocalDate.now().getYear();
                years = List.of(currentYear);
            }

            for (int year = startYear; year <= endYear; year++) {
                labels.add(String.valueOf(year));

                BigDecimal amount;
                if ("building".equalsIgnoreCase(type)) {
                    amount = statementRepository.sumBuildingExpensesByYear(buildingId, year);
                } else {
                    amount = allocationRepository.sumApartmentExpensesByYear(apartment.getId(), year);
                }

                values.add(amount != null ? amount.doubleValue() : 0.0);

            }
        }

        BigDecimal total = values.stream()
                .map(v -> BigDecimal.valueOf(v))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ChartResponseDTO(labels, values, total);
    }


    public StatementMiniChartDTO getStatementMiniChart(Integer userId) {

        Apartment apartment = findApartmentForUser(userId);
        if (apartment == null) return new StatementMiniChartDTO(0.0, 0.0, 0.0, List.of(), null);

        Integer buildingId = apartment.getBuilding().getId();

        List<CommonExpenseStatement> statements =
                statementRepository.findByBuildingIdOrderByStartDateDesc(buildingId);

        if (statements.isEmpty())
            return new StatementMiniChartDTO(0.0, 0.0, 0.0, List.of(), null);

        BigDecimal lastBD = statements.get(0).getTotal() == null ? BigDecimal.ZERO : statements.get(0).getTotal();
        BigDecimal prevBD = statements.size() > 1
                ? (statements.get(1).getTotal() == null ? BigDecimal.ZERO : statements.get(1).getTotal())
                : BigDecimal.ZERO;

        double lastAmount = lastBD.doubleValue();
        double prevAmount = prevBD.doubleValue();

        double percentage = 0.0;
        if (prevBD.compareTo(BigDecimal.ZERO) > 0) {
            percentage = lastBD.subtract(prevBD)
                    .divide(prevBD, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        String month = statements.get(0).getMonth();

        List<Double> last12 = statements.stream()
                .map(st -> st.getTotal() == null ? 0.0 : st.getTotal().doubleValue())
                .limit(12)
                .toList();

        return new StatementMiniChartDTO(lastAmount, prevAmount, percentage, last12, month);
    }




    public BigDecimal getUnpaidForUserApartment(Integer userId) {

        Apartment apartment = findApartmentForUser(userId);
        if (apartment == null) return BigDecimal.ZERO;

        BigDecimal sum = allocationRepository.sumUnpaidByApartment(apartment.getId());
        return sum != null ? sum : BigDecimal.ZERO;
    }


    public BuildingPendingDTO getBuildingPending(Integer buildingId) {
        double total = paymentRepository.findTotalUnpaidForBuilding(buildingId);
        List<String> months = paymentRepository.findUnpaidMonthsForBuilding(buildingId);

        return new BuildingPendingDTO(total, months);
    }

    public List<UserStatementDTO> getUserStatementTotals(Integer userId) {

        Apartment apartment = findApartmentForUser(userId);
        if (apartment == null) return List.of();

        Integer buildingId = apartment.getBuilding().getId();

        List<CommonExpenseStatement> statements =
                statementRepository.findByBuildingIdOrderByStartDateDesc(buildingId);

        if (statements.isEmpty()) return List.of();

        boolean isOwner    = apartment.getOwner()    != null && apartment.getOwner().getId().equals(userId);
        boolean isResident = apartment.getResident() != null && apartment.getResident().getId().equals(userId);
        boolean hasResident = apartment.getResident() != null;

        List<UserStatementDTO> result = new ArrayList<>();

        for (CommonExpenseStatement s : statements) {

            BigDecimal totalForApt = BigDecimal.ZERO;
            BigDecimal paidAmount  = BigDecimal.ZERO;

            List<CommonExpenseAllocation> allocations =
                    allocationRepository.findByStatementAndApartment(s, apartment);

            for (CommonExpenseAllocation alloc : allocations) {

                var category = alloc.getItem().getCategory();

                boolean shouldPay =
                        (hasResident && isResident && category != ExpenseCategory.OWNERS) ||
                                (hasResident && isOwner    && category == ExpenseCategory.OWNERS) ||
                                (!hasResident && isOwner);

                if (!shouldPay) continue;

                BigDecimal a = alloc.getAmount() == null ? BigDecimal.ZERO : alloc.getAmount();
                BigDecimal p = alloc.getPaidAmount() == null ? BigDecimal.ZERO : alloc.getPaidAmount();

                totalForApt = totalForApt.add(a);
                paidAmount  = paidAmount.add(p);

            }

            BigDecimal remaining = totalForApt.subtract(paidAmount);
            boolean isPaid = remaining.compareTo(BigDecimal.ZERO) <= 0;


            result.add(
                    UserStatementDTO.builder()
                            .statementId(s.getId())
                            .code(s.getCode())
                            .month(s.getMonth())
                            .totalForBuilding(s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .totalForApartment(totalForApt.doubleValue())
                            .paidAmount(paidAmount.doubleValue())
                            .remainingAmount(remaining.doubleValue())
                            .isPaid(isPaid)
                            .issueDate(s.getStartDate().toLocalDate())
                            .dueDate(s.getEndDate() != null ? s.getEndDate().toLocalDate() : null)

                            .status(s.getStatus().name())
                            .build()
            );
        }

        return result;
    }




}
