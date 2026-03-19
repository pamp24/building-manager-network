package com.buildingmanager.pmDashboard;

import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import com.buildingmanager.commonExpenseStatement.StatementStatus;
import com.buildingmanager.notification.NotificationRepository;
import com.buildingmanager.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PmDashboardService {

    private final BuildingRepository buildingRepository;
    private final CommonExpenseStatementRepository statementRepository;
    private final NotificationRepository notificationRepository;
    private final BuildingMemberRepository buildingMemberRepository;
    private final ApartmentRepository apartmentRepository;

    public PmDashboardDTO getDashboard(User user) {
        if (user.getCompany() == null) {
            throw new EntityNotFoundException("Property manager company not found");
        }

        List<Building> buildings = buildingRepository.findByCompany_Id(user.getCompany().getId());
        List<Integer> buildingIds = buildings.stream()
                .map(Building::getId)
                .toList();

        int totalBuildings = buildings.size();

        int totalApartments = buildings.stream()
                .mapToInt(b -> b.getApartmentsNum() != null ? b.getApartmentsNum() : 0)
                .sum();

        List<CommonExpenseStatement> statements = buildingIds.isEmpty()
                ? List.of()
                : statementRepository.findByBuilding_IdIn(buildingIds);

        double pendingAmount = statements.stream()
                .filter(s -> s.getStatus() == StatementStatus.ISSUED)
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                .sum();

        LocalDateTime now = LocalDateTime.now();

        double overdueAmount = statements.stream()
                .filter(s ->
                        s.getStatus() == StatementStatus.EXPIRED ||
                                (s.getStatus() == StatementStatus.ISSUED &&
                                        s.getEndDate() != null &&
                                        s.getEndDate().isBefore(now))
                )
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                .sum();

        List<AttentionBuildingDTO> attentionBuildings = buildings.stream()
                .map(building -> {
                    List<CommonExpenseStatement> buildingStatements = statements.stream()
                            .filter(s -> s.getBuilding() != null && s.getBuilding().getId().equals(building.getId()))
                            .toList();

                    long overdueCount = buildingStatements.stream()
                            .filter(s ->
                                    s.getStatus() == StatementStatus.EXPIRED ||
                                            (s.getStatus() == StatementStatus.ISSUED &&
                                                    s.getEndDate() != null &&
                                                    s.getEndDate().isBefore(now))
                            )
                            .count();

                    if (overdueCount > 0) {
                        return AttentionBuildingDTO.builder()
                                .buildingId(building.getId())
                                .name(building.getName())
                                .city(building.getCity())
                                .issue(overdueCount + " ληξιπρόθεσμες οφειλές")
                                .severity("danger")
                                .build();
                    }

                    if (buildingStatements.isEmpty()) {
                        return AttentionBuildingDTO.builder()
                                .buildingId(building.getId())
                                .name(building.getName())
                                .city(building.getCity())
                                .issue("Δεν έχει εκδοθεί παραστατικό")
                                .severity("warning")
                                .build();
                    }

                    return null;
                })
                .filter(item -> item != null)
                .limit(5)
                .toList();

        List<ActivityItemDTO> recentActivity = statements.stream()
                .sorted(Comparator.comparing(
                        CommonExpenseStatement::getCreatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(5)
                .map(s -> ActivityItemDTO.builder()
                        .title("Νέο παραστατικό")
                        .description(
                                (s.getBuilding() != null ? s.getBuilding().getName() : "Πολυκατοικία")
                                        + " - " + s.getCode()
                        )
                        .createdAt(s.getCreatedDate())
                        .build())
                .toList();

        List<DashboardNotificationDTO> notifications = notificationRepository
                .findByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .limit(5)
                .map(n -> DashboardNotificationDTO.builder()
                        .id(n.getId())
                        .title(n.getType())
                        .message(n.getMessage())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();

        return PmDashboardDTO.builder()
                .summary(SummaryDTO.builder()
                        .totalBuildings(totalBuildings)
                        .totalApartments(totalApartments)
                        .pendingAmount(pendingAmount)
                        .overdueAmount(overdueAmount)
                        .build())
                .attentionBuildings(attentionBuildings)
                .recentActivity(recentActivity)
                .notifications(notifications)
                .build();
    }

    public PmFinancialChartDTO getFinancialChart(User user, String period) {
        if (user.getCompany() == null) {
            throw new EntityNotFoundException("Property manager company not found");
        }

        List<Building> buildings = buildingRepository.findByCompany_Id(user.getCompany().getId());
        List<Integer> buildingIds = buildings.stream().map(Building::getId).toList();

        if (buildingIds.isEmpty()) {
            return PmFinancialChartDTO.builder()
                    .labels(List.of())
                    .issued(List.of())
                    .paid(List.of())
                    .overdue(List.of())
                    .build();
        }

        List<CommonExpenseStatement> statements = statementRepository.findByBuilding_IdIn(buildingIds);

        List<String> labels = new ArrayList<>();
        List<Double> issued = new ArrayList<>();
        List<Double> paid = new ArrayList<>();
        List<Double> overdue = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();

        String safePeriod = (period == null || period.isBlank()) ? "month" : period;

        switch (safePeriod) {
            case "quarter" -> {
                for (int q = 1; q <= 4; q++) {
                    int startMonth = (q - 1) * 3 + 1;
                    int endMonth = startMonth + 2;

                    labels.add("Q" + q);

                    double issuedValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> {
                                int m = s.getStartDate().getMonthValue();
                                return m >= startMonth && m <= endMonth;
                            })
                            .filter(s -> s.getStatus() == StatementStatus.ISSUED
                                    || s.getStatus() == StatementStatus.PAID
                                    || s.getStatus() == StatementStatus.EXPIRED)
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    double paidValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> {
                                int m = s.getStartDate().getMonthValue();
                                return m >= startMonth && m <= endMonth;
                            })
                            .filter(s -> s.getStatus() == StatementStatus.PAID)
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    double overdueValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> {
                                int m = s.getStartDate().getMonthValue();
                                return m >= startMonth && m <= endMonth;
                            })
                            .filter(s -> s.getStatus() == StatementStatus.EXPIRED
                                    || (s.getStatus() == StatementStatus.ISSUED
                                    && s.getEndDate() != null
                                    && s.getEndDate().isBefore(now)))
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    issued.add(issuedValue);
                    paid.add(paidValue);
                    overdue.add(overdueValue);
                }
            }

            case "halfyear" -> {
                for (int h = 1; h <= 2; h++) {
                    int startMonth = h == 1 ? 1 : 7;
                    int endMonth = h == 1 ? 6 : 12;

                    labels.add(h == 1 ? "Α' Εξάμηνο" : "Β' Εξάμηνο");

                    double issuedValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> {
                                int m = s.getStartDate().getMonthValue();
                                return m >= startMonth && m <= endMonth;
                            })
                            .filter(s -> s.getStatus() == StatementStatus.ISSUED
                                    || s.getStatus() == StatementStatus.PAID
                                    || s.getStatus() == StatementStatus.EXPIRED)
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    double paidValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> {
                                int m = s.getStartDate().getMonthValue();
                                return m >= startMonth && m <= endMonth;
                            })
                            .filter(s -> s.getStatus() == StatementStatus.PAID)
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    double overdueValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> {
                                int m = s.getStartDate().getMonthValue();
                                return m >= startMonth && m <= endMonth;
                            })
                            .filter(s -> s.getStatus() == StatementStatus.EXPIRED
                                    || (s.getStatus() == StatementStatus.ISSUED
                                    && s.getEndDate() != null
                                    && s.getEndDate().isBefore(now)))
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    issued.add(issuedValue);
                    paid.add(paidValue);
                    overdue.add(overdueValue);
                }
            }

            case "year" -> {
                labels.add(String.valueOf(year));

                double issuedValue = statements.stream()
                        .filter(s -> s.getStartDate() != null)
                        .filter(s -> s.getStartDate().getYear() == year)
                        .filter(s -> s.getStatus() == StatementStatus.ISSUED
                                || s.getStatus() == StatementStatus.PAID
                                || s.getStatus() == StatementStatus.EXPIRED)
                        .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                        .sum();

                double paidValue = statements.stream()
                        .filter(s -> s.getStartDate() != null)
                        .filter(s -> s.getStartDate().getYear() == year)
                        .filter(s -> s.getStatus() == StatementStatus.PAID)
                        .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                        .sum();

                double overdueValue = statements.stream()
                        .filter(s -> s.getStartDate() != null)
                        .filter(s -> s.getStartDate().getYear() == year)
                        .filter(s -> s.getStatus() == StatementStatus.EXPIRED
                                || (s.getStatus() == StatementStatus.ISSUED
                                && s.getEndDate() != null
                                && s.getEndDate().isBefore(now)))
                        .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                        .sum();

                issued.add(issuedValue);
                paid.add(paidValue);
                overdue.add(overdueValue);
            }

            case "month" -> {
                for (int month = 1; month <= 12; month++) {
                    labels.add(Month.of(month).getDisplayName(TextStyle.SHORT, new Locale("el", "GR")));

                    final int currentMonth = month;

                    double issuedValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> s.getStartDate().getMonthValue() == currentMonth)
                            .filter(s -> s.getStatus() == StatementStatus.ISSUED
                                    || s.getStatus() == StatementStatus.PAID
                                    || s.getStatus() == StatementStatus.EXPIRED)
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    double paidValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> s.getStartDate().getMonthValue() == currentMonth)
                            .filter(s -> s.getStatus() == StatementStatus.PAID)
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    double overdueValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> s.getStartDate().getMonthValue() == currentMonth)
                            .filter(s -> s.getStatus() == StatementStatus.EXPIRED
                                    || (s.getStatus() == StatementStatus.ISSUED
                                    && s.getEndDate() != null
                                    && s.getEndDate().isBefore(now)))
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    issued.add(issuedValue);
                    paid.add(paidValue);
                    overdue.add(overdueValue);
                }
            }

            default -> {
                for (int month = 1; month <= 12; month++) {
                    labels.add(Month.of(month).getDisplayName(TextStyle.SHORT, new Locale("el", "GR")));

                    final int currentMonth = month;

                    double issuedValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> s.getStartDate().getMonthValue() == currentMonth)
                            .filter(s -> s.getStatus() == StatementStatus.ISSUED
                                    || s.getStatus() == StatementStatus.PAID
                                    || s.getStatus() == StatementStatus.EXPIRED)
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    double paidValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> s.getStartDate().getMonthValue() == currentMonth)
                            .filter(s -> s.getStatus() == StatementStatus.PAID)
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    double overdueValue = statements.stream()
                            .filter(s -> s.getStartDate() != null)
                            .filter(s -> s.getStartDate().getYear() == year)
                            .filter(s -> s.getStartDate().getMonthValue() == currentMonth)
                            .filter(s -> s.getStatus() == StatementStatus.EXPIRED
                                    || (s.getStatus() == StatementStatus.ISSUED
                                    && s.getEndDate() != null
                                    && s.getEndDate().isBefore(now)))
                            .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                            .sum();

                    issued.add(issuedValue);
                    paid.add(paidValue);
                    overdue.add(overdueValue);
                }
            }
        }

        return PmFinancialChartDTO.builder()
                .labels(labels)
                .issued(issued)
                .paid(paid)
                .overdue(overdue)
                .build();
    }

    public PmExpenseCollectionRateDTO getExpenseCollectionRate(User user) {
        if (user.getCompany() == null) {
            throw new EntityNotFoundException("Property manager company not found");
        }

        List<Building> buildings = buildingRepository.findByCompany_Id(user.getCompany().getId());
        List<Integer> buildingIds = buildings.stream()
                .map(Building::getId)
                .toList();

        if (buildingIds.isEmpty()) {
            return PmExpenseCollectionRateDTO.builder()
                    .collectionRate(0)
                    .issuedAmount(0.0)
                    .paidAmount(0.0)
                    .overdueAmount(0.0)
                    .build();
        }

        List<CommonExpenseStatement> statements = statementRepository.findByBuilding_IdIn(buildingIds);

        LocalDateTime now = LocalDateTime.now();

        double issuedAmount = statements.stream()
                .filter(s -> s.getStatus() == StatementStatus.ISSUED
                        || s.getStatus() == StatementStatus.PAID
                        || s.getStatus() == StatementStatus.EXPIRED)
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                .sum();

        double paidAmount = statements.stream()
                .filter(s -> s.getStatus() == StatementStatus.PAID)
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                .sum();

        double overdueAmount = statements.stream()
                .filter(s -> s.getStatus() == StatementStatus.EXPIRED
                        || (s.getStatus() == StatementStatus.ISSUED
                        && s.getEndDate() != null
                        && s.getEndDate().isBefore(now)))
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                .sum();

        int collectionRate = issuedAmount > 0
                ? (int) Math.round((paidAmount / issuedAmount) * 100)
                : 0;

        return PmExpenseCollectionRateDTO.builder()
                .collectionRate(collectionRate)
                .issuedAmount(issuedAmount)
                .paidAmount(paidAmount)
                .overdueAmount(overdueAmount)
                .build();
    }

    public List<PmAttentionBuildingDTO> getAttentionBuildings(User user) {
        if (user.getCompany() == null) {
            throw new EntityNotFoundException("Property manager company not found");
        }

        List<Building> buildings = buildingRepository.findByCompany_Id(user.getCompany().getId());

        if (buildings.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeMonthsAgo = now.minusMonths(3);

        List<PmAttentionBuildingDTO> result = new ArrayList<>();

        for (Building building : buildings) {
            List<CommonExpenseStatement> statements = statementRepository.findByBuilding_Id(building.getId());

            double issuedAmount = statements.stream()
                    .filter(s -> s.getStatus() == StatementStatus.ISSUED
                            || s.getStatus() == StatementStatus.PAID
                            || s.getStatus() == StatementStatus.EXPIRED)
                    .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                    .sum();

            double paidAmount = statements.stream()
                    .filter(s -> s.getStatus() == StatementStatus.PAID)
                    .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                    .sum();

            double overdueAmount = statements.stream()
                    .filter(s -> s.getStatus() == StatementStatus.EXPIRED
                            || (s.getStatus() == StatementStatus.ISSUED
                            && s.getEndDate() != null
                            && s.getEndDate().isBefore(now)))
                    .mapToDouble(s -> s.getTotal() != null ? s.getTotal().doubleValue() : 0.0)
                    .sum();

            int collectionRate = issuedAmount > 0
                    ? (int) Math.round((paidAmount / issuedAmount) * 100)
                    : 0;

            boolean hasRecentStatement = statements.stream()
                    .filter(s -> s.getStartDate() != null)
                    .anyMatch(s -> !s.getStartDate().isBefore(threeMonthsAgo));

            String riskLevel = null;
            String reason = null;

            if (overdueAmount > 3000) {
                riskLevel = "HIGH";
                reason = "Υψηλά ληξιπρόθεσμα";
            } else if (collectionRate < 50 && issuedAmount > 0) {
                riskLevel = "HIGH";
                reason = "Χαμηλό ποσοστό είσπραξης";
            } else if (!hasRecentStatement) {
                riskLevel = "MEDIUM";
                reason = "Δεν έχουν εκδοθεί πρόσφατα κοινόχρηστα";
            } else if (overdueAmount > 1000) {
                riskLevel = "MEDIUM";
                reason = "Αυξημένα ληξιπρόθεσμα";
            } else if (collectionRate < 70 && issuedAmount > 0) {
                riskLevel = "MEDIUM";
                reason = "Μέτριο ποσοστό είσπραξης";
            }

            if (riskLevel != null) {
                result.add(PmAttentionBuildingDTO.builder()
                        .buildingId(building.getId())
                        .buildingName(building.getName())
                        .buildingCode(building.getBuildingCode())
                        .overdueAmount(overdueAmount)
                        .collectionRate(collectionRate)
                        .riskLevel(riskLevel)
                        .reason(reason)
                        .build());
            }
        }

        return result.stream()
                .sorted((a, b) -> {
                    int riskCompare = compareRiskLevel(b.getRiskLevel(), a.getRiskLevel());
                    if (riskCompare != 0) {
                        return riskCompare;
                    }
                    return Double.compare(b.getOverdueAmount(), a.getOverdueAmount());
                })
                .toList();
    }

    private int compareRiskLevel(String first, String second) {
        return Integer.compare(getRiskPriority(first), getRiskPriority(second));
    }

    private int getRiskPriority(String riskLevel) {
        return switch (riskLevel) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    public PmMembershipStatsDTO getMembershipStats(User user) {
        if (user.getCompany() == null) {
            throw new EntityNotFoundException("Property manager company not found");
        }

        List<Building> buildings = buildingRepository.findByCompany_Id(user.getCompany().getId());

        List<Integer> buildingIds = buildings.stream()
                .map(Building::getId)
                .toList();

        if (buildingIds.isEmpty()) {
            return PmMembershipStatsDTO.builder()
                    .pendingInvites(0)
                    .pendingJoinRequests(0)
                    .joinedMembers(0)
                    .unassignedApartments(0)
                    .build();
        }

        long pendingInvites = buildingMemberRepository
                .countByBuilding_IdInAndStatus(buildingIds, BuildingMemberStatus.INVITED);

        long pendingJoinRequests = buildingMemberRepository
                .countByBuilding_IdInAndStatus(buildingIds, BuildingMemberStatus.PENDING);

        long joinedMembers = buildingMemberRepository
                .countByBuilding_IdInAndStatusAndRole_NameIn(
                        buildingIds,
                        BuildingMemberStatus.JOINED,
                        List.of("Owner", "Resident", "BuildingManager")
                );

        long unassignedApartments = apartmentRepository
                .countUnassignedApartmentsByBuildingIds(buildingIds);

        return PmMembershipStatsDTO.builder()
                .pendingInvites(pendingInvites)
                .pendingJoinRequests(pendingJoinRequests)
                .joinedMembers(joinedMembers)
                .unassignedApartments(unassignedApartments)
                .build();
    }

    public List<PmBuildingManagerRowDTO> getBuildingManagers(User user) {
        if (user.getCompany() == null) {
            throw new EntityNotFoundException("Property manager company not found");
        }

        List<Building> buildings = buildingRepository.findByCompany_Id(user.getCompany().getId());

        return buildings.stream()
                .map(building -> {
                    User manager = building.getManager();

                    return PmBuildingManagerRowDTO.builder()
                            .buildingId(building.getId())
                            .buildingName(building.getName())
                            .buildingCode(building.getBuildingCode())
                            .city(building.getCity())
                            .managerId(manager != null ? manager.getId() : null)
                            .managerFullName(manager != null ? manager.getFullName() : null)
                            .managerEmail(manager != null ? manager.getEmail() : null)
                            .managerPhone(manager != null ? manager.getPhoneNumber() : null)
                            .managerAssigned(manager != null)
                            .build();
                })
                .toList();
    }
}