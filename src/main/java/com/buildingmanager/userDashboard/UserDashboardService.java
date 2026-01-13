package com.buildingmanager.userDashboard;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationDTO;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationMapper;
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

        double totalDue = 0.0;

        // 4) ΥΠΟΛΟΓΙΣΜΟΣ ΠΟΣΟΥ ΠΟΥ ΧΡΩΣΤΑΕΙ
        for (CommonExpenseAllocation alloc : allocations) {

            double amount = alloc.getAmount() - (alloc.getPaidAmount() == null ? 0.0 : alloc.getPaidAmount());
            if (amount <= 0) continue;

            ExpenseCategory category = alloc.getItem().getCategory();
            boolean hasResident = apartment.getResident() != null;

            // CASE 1: Υπάρχει resident
            if (hasResident) {

                // Resident: όλα εκτός OWNERS
                if (isResident && category != ExpenseCategory.OWNERS) {
                    totalDue += amount;
                }

                // Owner: μόνο OWNERS
                if (isOwner && category == ExpenseCategory.OWNERS) {
                    totalDue += amount;
                }

                continue;
            }

            // CASE 2: Δεν υπάρχει resident → owner πληρώνει ΟΛΑ
            if (!hasResident && isOwner) {
                totalDue += amount;
            }
        }


        log.info("Final amount due: {}", totalDue);

        return UserDashboardSummaryDTO.builder()
                .latestDebt(BigDecimal.valueOf(totalDue))
                .statementId(lastStatement.getId())
                .statementMonth(lastStatement.getMonth())
                .role(userRole)
                .build();
    }

    public List<UserStatementHistoryDTO> getHistory(Integer userId) {

        log.info("Loading dashboard history for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find correct apartment
        Apartment apartment = apartmentRepository.findByResident_Id(userId).stream().findFirst()
                .orElse(apartmentRepository.findByOwner_Id(userId).stream().findFirst().orElse(null));

        if (apartment == null) {
            log.warn("User {} has no apartment -> empty history", userId);
            return List.of();
        }

        // Fetch all statements for this building
        List<CommonExpenseStatement> statements =
                statementRepository.findByBuildingId(apartment.getBuilding().getId());

        // Sort latest → oldest using start_date
        statements.sort((a, b) ->
                b.getStartDate().compareTo(a.getStartDate())
        );

        // Keep last 12
        statements = statements.stream().limit(12).toList();

        // Map to DTO with user-specific total
        return statements.stream()
                .map(s -> new UserStatementHistoryDTO(
                        s.getId(),
                        s.getMonth(),
                        calculateUserTotalForStatement(s, apartment, user)
                ))
                .toList();
    }

    private double calculateUserTotalForStatement(
            CommonExpenseStatement statement,
            Apartment apartment,
            User user
    ) {
        List<CommonExpenseAllocation> allocations =
                allocationRepository.findByStatementAndApartment(statement, apartment);

        double total = 0.0;

        boolean isOwner = apartment.getOwner() != null &&
                apartment.getOwner().getId().equals(user.getId());

        boolean isResident = apartment.getResident() != null &&
                apartment.getResident().getId().equals(user.getId());

        boolean hasResident = apartment.getResident() != null;

        for (CommonExpenseAllocation alloc : allocations) {

            double amount = alloc.getAmount() -
                    (alloc.getPaidAmount() == null ? 0.0 : alloc.getPaidAmount());

            if (amount <= 0) continue;

            ExpenseCategory category = alloc.getItem().getCategory();

            if (hasResident) {

                // Resident: όλα εκτός OWNERS
                if (isResident && category != ExpenseCategory.OWNERS) {
                    total += amount;
                }

                // Owner: μόνο OWNERS
                if (isOwner && category == ExpenseCategory.OWNERS) {
                    total += amount;
                }

            } else {
                // No resident → Owner πληρώνει όλα
                if (isOwner) total += amount;
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
        if (apartment == null) return new ChartResponseDTO(List.of(), List.of(), 0.0);

        Integer buildingId = apartment.getBuilding().getId();

        List<String> labels;
        List<Double> values = new ArrayList<>();

        if (period.equals("month")) {

            labels = List.of("Ιαν", "Φεβ", "Μαρτ", "Απρ", "Μαϊ", "Ιουν", "Ιουλ", "Αυγ", "Σεπ", "Οκτ", "Νοε", "Δεκ");

            for (int month = 1; month <= 12; month++) {
                double amount;

                if (type.equals("building")) {
                    amount = statementRepository.sumBuildingExpensesByMonth(buildingId, month);
                } else {
                    amount = allocationRepository.sumApartmentExpensesByMonth(apartment.getId(), month);
                }

                values.add(amount);
            }

        } else {

            labels = List.of("2021", "2022", "2023", "2024", "2025");

            for (String yearStr : labels) {
                int year = Integer.parseInt(yearStr);
                double amount;

                if (type.equals("building")) {
                    amount = statementRepository.sumBuildingExpensesByYear(buildingId, year);
                } else {
                    amount = allocationRepository.sumApartmentExpensesByYear(apartment.getId(), year);
                }

                values.add(amount);
            }
        }

        double total = values.stream().mapToDouble(Double::doubleValue).sum();

        return new ChartResponseDTO(labels, values, total);
    }

    public StatementMiniChartDTO getStatementMiniChart(Integer userId) {

        Apartment apartment = findApartmentForUser(userId);
        if (apartment == null) return new StatementMiniChartDTO(0, 0, 0, List.of(), null);

        Integer buildingId = apartment.getBuilding().getId();

        // Φέρε όλα τα statements ταξινομημένα με DESC
        List<CommonExpenseStatement> statements =
                statementRepository.findByBuildingIdOrderByStartDateDesc(buildingId);

        if (statements.isEmpty())
            return new StatementMiniChartDTO(0, 0, 0, List.of(), null);

        double lastAmount = statements.get(0).getTotal();
        double prevAmount = statements.size() > 1 ? statements.get(1).getTotal() : 0;

        double percentage = 0;
        if (prevAmount > 0) {
            percentage = ((lastAmount - prevAmount) / prevAmount) * 100;
        }

        String month = statements.get(0).getMonth();

        // Πάρε τα τελευταία 12 amounts
        List<Double> last12 = statements.stream()
                .map(CommonExpenseStatement::getTotal)
                .limit(12)
                .collect(Collectors.toList());

        return new StatementMiniChartDTO(lastAmount, prevAmount, percentage, last12, month);
    }

    public Double getUnpaidForUserApartment(Integer userId) {

        Apartment apartment = findApartmentForUser(userId);
        if (apartment == null) return 0.0;

        return allocationRepository.sumUnpaidByApartment(apartment.getId());
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

            double totalForApt = 0.0;
            double paidAmount  = 0.0;

            List<CommonExpenseAllocation> allocations =
                    allocationRepository.findByStatementAndApartment(s, apartment);

            for (CommonExpenseAllocation alloc : allocations) {

                var category = alloc.getItem().getCategory();

                boolean shouldPay =
                        (hasResident && isResident && category != ExpenseCategory.OWNERS) ||
                                (hasResident && isOwner    && category == ExpenseCategory.OWNERS) ||
                                (!hasResident && isOwner);

                if (!shouldPay) continue;

                totalForApt += alloc.getAmount();
                paidAmount  += alloc.getPaidAmount() == null ? 0 : alloc.getPaidAmount();
            }

            double remaining = totalForApt - paidAmount;

            boolean isPaid = remaining <= 0;


            result.add(
                    UserStatementDTO.builder()
                            .statementId(s.getId())
                            .code(s.getCode())
                            .month(s.getMonth())
                            .totalForBuilding(s.getTotal())

                            .totalForApartment(totalForApt)
                            .paidAmount(paidAmount)
                            .remainingAmount(remaining)
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
