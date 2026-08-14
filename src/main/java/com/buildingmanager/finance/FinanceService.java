package com.buildingmanager.finance;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import com.buildingmanager.commonExpenseItem.CommonExpenseItemRepository;
import com.buildingmanager.commonExpenseItem.ExpenseCategory;
import com.buildingmanager.commonExpenseItem.ExpenseCategorySummaryDTO;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import com.buildingmanager.payment.PaymentDTO;
import com.buildingmanager.payment.PaymentRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import com.buildingmanager.userDashboard.UserStatementDTO;
import com.buildingmanager.userDashboard.UserDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceService {

    private final BuildingRepository buildingRepository;
    private final ApartmentRepository apartmentRepository;
    private final CommonExpenseStatementRepository statementRepository;
    private final CommonExpenseAllocationRepository allocationRepository;
    private final CommonExpenseItemRepository expenseItemRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final UserDashboardService userDashboardService;

    public BuildingFinanceDTO getBuildingFinance(Integer buildingId, User currentUser) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Building not found"));

        String userRole = resolveUserRole(currentUser, buildingId);
        boolean isManager = "BuildingManager".equals(userRole) || "PropertyManager".equals(userRole) || "Admin".equals(userRole);

        List<CommonExpenseStatement> statements = statementRepository.findByBuildingIdOrderByStartDateDesc(buildingId);
        CommonExpenseStatement lastStatement = statements.isEmpty() ? null : statements.get(0);

        Apartment apartment = findApartmentForUser(currentUser.getId());

        BigDecimal myPending = BigDecimal.ZERO;
        BigDecimal myPaid = BigDecimal.ZERO;

        if (apartment != null && lastStatement != null) {
            List<CommonExpenseAllocation> allocations = allocationRepository.findByStatementAndApartment(lastStatement, apartment);
            for (CommonExpenseAllocation alloc : allocations) {
                if (shouldUserPay(alloc, apartment, currentUser)) {
                    BigDecimal amount = alloc.getAmount() == null ? BigDecimal.ZERO : alloc.getAmount();
                    BigDecimal paid = alloc.getPaidAmount() == null ? BigDecimal.ZERO : alloc.getPaidAmount();
                    myPaid = myPaid.add(paid);
                    myPending = myPending.add(amount.subtract(paid));
                }
            }
        }

        List<UserStatementDTO> userStatements = userDashboardService.getUserStatementTotals(currentUser.getId());
        List<PaymentDTO> recentPayments = paymentRepository.findRecentByBuilding(buildingId, PageRequest.of(0, 10));

        YearMonth currentYm = YearMonth.now();
        LocalDateTime startOfMonth = currentYm.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentYm.atEndOfMonth().atTime(LocalTime.MAX);
        List<ExpenseCategorySummaryDTO> currentMonthExpenses = expenseItemRepository.findCategoryTotals(buildingId, startOfMonth, endOfMonth);

        BuildingFinanceDTO.BuildingFinanceDTOBuilder builder = BuildingFinanceDTO.builder()
                .buildingName(building.getName() != null ? building.getName() : building.getStreet1() + " " + building.getStNumber1() + ", " + building.getCity())
                .buildingCode(building.getBuildingCode())
                .userRole(userRole)
                .managerView(isManager)
                .myPending(myPending)
                .myPaid(myPaid)
                .lastStatementId(lastStatement != null ? lastStatement.getId() : null)
                .lastStatementMonth(lastStatement != null ? lastStatement.getMonth() : null)
                .statements(userStatements)
                .recentPayments(recentPayments)
                .currentMonthExpenses(currentMonthExpenses);

        if (isManager) {
            BigDecimal totalPending = statementRepository.sumUnpaidAmountByBuilding(buildingId);
            BigDecimal totalCollected = statementRepository.sumPaidAmountByBuilding(buildingId);
            if (totalPending == null) totalPending = BigDecimal.ZERO;
            if (totalCollected == null) totalCollected = BigDecimal.ZERO;
            BigDecimal total = totalPending.add(totalCollected);
            double collectionRate = total.compareTo(BigDecimal.ZERO) > 0
                    ? totalCollected.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            long totalApartments = apartmentRepository.countByBuilding_Id(buildingId);
            long totalMembers = building.getMembers() != null ? building.getMembers().size() : 0;

            List<MemberPaymentStatus> memberStatuses = buildMemberPaymentStatuses(buildingId, lastStatement);

            builder.totalBuildingPending(totalPending)
                    .totalBuildingCollected(totalCollected)
                    .collectionRate(collectionRate)
                    .totalApartments((int) totalApartments)
                    .totalMembers((int) totalMembers)
                    .memberPaymentStatuses(memberStatuses);
        }

        return builder.build();
    }

    private String resolveUserRole(User user, Integer buildingId) {
        com.buildingmanager.role.Role role = user.getRole();
        if (role != null) {
            String roleName = role.getName();
            if ("ROLE_ADMIN".equals(roleName) || "Admin".equals(roleName)) {
                return "Admin";
            }
        }
        List<Apartment> owned = apartmentRepository.findByOwner_Id(user.getId());
        for (Apartment a : owned) {
            if (a.getBuilding().getId().equals(buildingId)) return "Owner";
        }
        List<Apartment> resident = apartmentRepository.findByResident_Id(user.getId());
        for (Apartment a : resident) {
            if (a.getBuilding().getId().equals(buildingId)) return "Resident";
        }
        return "Owner";
    }

    private Apartment findApartmentForUser(Integer userId) {
        List<Apartment> residentApts = apartmentRepository.findByResident_Id(userId);
        if (!residentApts.isEmpty()) return residentApts.get(0);
        List<Apartment> ownerApts = apartmentRepository.findByOwner_Id(userId);
        if (!ownerApts.isEmpty()) return ownerApts.get(0);
        return null;
    }

    private boolean shouldUserPay(CommonExpenseAllocation alloc, Apartment apartment, User user) {
        if (apartment == null) return false;
        boolean isOwner = apartment.getOwner() != null && apartment.getOwner().getId().equals(user.getId());
        boolean isResident = apartment.getResident() != null && apartment.getResident().getId().equals(user.getId());
        boolean hasResident = apartment.getResident() != null;
        ExpenseCategory category = alloc.getItem().getCategory();
        if (hasResident) {
            if (isResident && category != ExpenseCategory.OWNERS) return true;
            if (isOwner && category == ExpenseCategory.OWNERS) return true;
            return false;
        }
        return isOwner;
    }

    private List<MemberPaymentStatus> buildMemberPaymentStatuses(Integer buildingId, CommonExpenseStatement lastStatement) {
        List<MemberPaymentStatus> result = new ArrayList<>();
        if (lastStatement == null) return result;

        List<Apartment> apartments = apartmentRepository.findAllByBuilding_Id(buildingId);
        for (Apartment apt : apartments) {
            List<CommonExpenseAllocation> allocations = allocationRepository.findByStatementAndApartment(lastStatement, apt);
            BigDecimal totalDue = BigDecimal.ZERO;
            BigDecimal totalPaid = BigDecimal.ZERO;

            for (CommonExpenseAllocation alloc : allocations) {
                BigDecimal amount = alloc.getAmount() == null ? BigDecimal.ZERO : alloc.getAmount();
                BigDecimal paid = alloc.getPaidAmount() == null ? BigDecimal.ZERO : alloc.getPaidAmount();
                totalDue = totalDue.add(amount);
                totalPaid = totalPaid.add(paid);
            }

            User owner = apt.getOwner();
            User resident = apt.getResident();
            User primaryUser = resident != null ? resident : owner;

            String status;
            BigDecimal remaining = totalDue.subtract(totalPaid);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                status = "PAID";
            } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                status = "PARTIALLY_PAID";
            } else {
                status = "UNPAID";
            }

            result.add(MemberPaymentStatus.builder()
                    .userId(primaryUser != null ? primaryUser.getId() : null)
                    .fullName(primaryUser != null ? primaryUser.getFirstName() + " " + primaryUser.getLastName() : "—")
                    .apartmentId(apt.getId())
                    .apartmentNumber(apt.getNumber() != null ? apt.getNumber().toString() : "—")
                    .amountDue(totalDue)
                    .amountPaid(totalPaid)
                    .status(status)
                    .build());
        }
        return result;
    }
}
