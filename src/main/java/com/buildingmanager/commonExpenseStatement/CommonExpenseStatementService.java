package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import com.buildingmanager.commonExpenseItem.CommonExpenseItem;
import com.buildingmanager.commonExpenseItem.ExpenseCategory;
import com.buildingmanager.notification.NotificationService;
import com.buildingmanager.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class CommonExpenseStatementService {

    private final CommonExpenseStatementRepository commonExpenseStatementRepository;
    private final ApartmentRepository apartmentRepository;
    private final CommonExpenseAllocationRepository commonExpenseAllocationRepository;
    private final NotificationService notificationService;
    private final BuildingRepository buildingRepository;
    private final ObjectMapper objectMapper;

    private static BigDecimal bd(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal pct100(BigDecimal percent) {
        // 15 -> 0.15
        return bd(percent).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }

    private static BigDecimal milli(Double mills) {
        // 125 -> 0.125 (χιλιοστά)
        return mills == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(mills).divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
    }

    private static BigDecimal s2(BigDecimal v) {
        return bd(v).setScale(2, RoundingMode.HALF_UP);
    }


    @Transactional
    public CommonExpenseStatement createAndSend(CommonExpenseStatement statement) {

        // 1) buildingId
        Integer buildingId = statement.getBuilding().getId();

        // 2) totals
        BigDecimal subTotal = statement.getItems().stream()
                .map(i -> bd(i.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = subTotal.multiply(pct100(statement.getDiscountPercent()));
        BigDecimal taxed    = subTotal.subtract(discount).multiply(pct100(statement.getTaxPercent()));
        BigDecimal total    = subTotal.subtract(discount).add(taxed);

        // Κλειδώνουμε 2 δεκαδικά πριν το save
        statement.setSubTotal(s2(subTotal));
        statement.setTotal(s2(total));

        // 3) sequenceNumber
        Integer maxSeq = commonExpenseStatementRepository.findMaxSequenceByBuilding(buildingId);
        int nextSeq = (maxSeq == null) ? 1 : maxSeq + 1;
        statement.setSequenceNumber(nextSeq);

        // 4) link items
        statement.getItems().forEach(i -> i.setStatement(statement));

        // 5) status
        if (statement.getStatus() == null) {
            statement.setStatus(StatementStatus.ISSUED);
        }

        // 6) save statement
        CommonExpenseStatement saved = commonExpenseStatementRepository.save(statement);

        // 7) apartments
        List<Apartment> apartments = apartmentRepository.findAllByBuilding_Id(buildingId);

        // 8) allocations
        for (CommonExpenseItem item : saved.getItems()) {
            BigDecimal itemTotal = bd(item.getPrice());
            int n = apartments.size();

            // Για EQUAL / OTHER / SPECIAL / OWNERS / BOILER: μοιράζουμε ακριβώς μέχρι cent
            BigDecimal baseEqual = itemTotal.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
            BigDecimal remainder = itemTotal.subtract(baseEqual.multiply(BigDecimal.valueOf(n)));
            int extraCents = remainder.movePointRight(2).intValue(); // πόσα +0.01

            for (int idx = 0; idx < apartments.size(); idx++) {
                Apartment apt = apartments.get(idx);

                BigDecimal share;

                switch (item.getCategory()) {
                    case COMMON -> share = itemTotal.multiply(milli(apt.getCommonPercent()));
                    case ELEVATOR -> share = itemTotal.multiply(milli(apt.getElevatorPercent()));
                    case HEATING -> share = itemTotal.multiply(milli(apt.getHeatingPercent()));
                    case EQUAL, OTHER, SPECIAL, OWNERS, BOILER -> {
                        share = baseEqual;
                        if (idx < extraCents) share = share.add(BigDecimal.valueOf(0.01));
                    }
                    default -> share = BigDecimal.ZERO;
                }

                // Κλειδώνουμε 2 δεκαδικά
                share = s2(share);

                CommonExpenseAllocation allocation = CommonExpenseAllocation.builder()
                        .statement(saved)
                        .item(item)
                        .apartment(apt)
                        .commonPercent(apt.getCommonPercent())
                        .elevatorPercent(apt.getElevatorPercent())
                        .heatingPercent(apt.getHeatingPercent())
                        .amount(share)
                        .paidAmount(BigDecimal.ZERO)
                        .isPaid(false)
                        .status("UNPAID")
                        .build();

                // ποιος πληρώνει
                if (apt.getResident() != null) {
                    allocation.setUser(item.getCategory() == ExpenseCategory.OWNERS ? apt.getOwner() : apt.getResident());
                } else {
                    allocation.setUser(apt.getOwner());
                }

                commonExpenseAllocationRepository.save(allocation);
            }
        }


        // 9) Notifications ΜΙΑ φορά σε όλους τους χρήστες
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Building not found"));

        Set<User> receivers = new HashSet<>();
        for (Apartment a : apartments) {
            if (a.getOwner() != null) receivers.add(a.getOwner());
            if (a.getResident() != null) receivers.add(a.getResident());
        }
        if (building.getManager() != null) receivers.add(building.getManager());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "buildingId", buildingId,
                    "statementId", saved.getId(),
                    "code", saved.getCode(),
                    "month", saved.getMonth(),
                    "tab", "invoice"
            ));
        } catch (Exception e) {
            payload = null;
        }

        String message = "Εκδόθηκε νέο παραστατικό: " + saved.getCode() + " (" + saved.getMonth() + ")";

        for (User user : receivers) {
            notificationService.create(user, "NEW_STATEMENT", message, payload);
        }

        return saved;
    }



    @Transactional
    public CommonExpenseStatement saveDraft(CommonExpenseStatement statement) {
        Integer buildingId = statement.getBuilding().getId();

        // Υπολογισμός sequence
        Integer maxSeq = commonExpenseStatementRepository.findMaxSequenceByBuilding(buildingId);
        int nextSeq = (maxSeq == null) ? 1 : maxSeq + 1;
        statement.setSequenceNumber(nextSeq);

        // Υπολογισμός συνολικών ποσών
        BigDecimal subTotal = statement.getItems().stream()
                .map(i -> bd(i.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountPercent = bd(statement.getDiscountPercent());
        BigDecimal taxPercent = bd(statement.getTaxPercent());

        BigDecimal discount = subTotal
                .multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal taxed = subTotal.subtract(discount)
                .multiply(taxPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal total = subTotal.subtract(discount).add(taxed);

        statement.setSubTotal(s2(subTotal));
        statement.setTotal(s2(total));

        // Status = DRAFT
        statement.setStatus(StatementStatus.DRAFT);

        // Συνδέουμε items
        statement.getItems().forEach(i -> i.setStatement(statement));
        statement.setStatus(StatementStatus.DRAFT);
        return commonExpenseStatementRepository.save(statement);
    }

    public List<CommonExpenseStatement> getAll() {
        return commonExpenseStatementRepository.findAll();
    }
    public List<CommonExpenseStatement> getAllActive() {
        return commonExpenseStatementRepository.getAllActive();
    }

    public CommonExpenseStatement getById(Integer id) {
        return commonExpenseStatementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε κατάσταση με id " + id));
    }

    public List<CommonExpenseStatementDTO> getStatementsByBuilding(Integer buildingId) {
        List<CommonExpenseStatement> statements = commonExpenseStatementRepository.findByBuildingId(buildingId);
        LocalDateTime now = LocalDateTime.now();

        return statements.stream().map(s -> {

            Boolean isPaid = s.getIsPaid() != null ? s.getIsPaid() : false;

            if (!isPaid && s.getEndDate() != null
                    && s.getEndDate().isBefore(now)
                    && s.getStatus() == StatementStatus.ISSUED) {
                s.setStatus(StatementStatus.EXPIRED);
                commonExpenseStatementRepository.save(s);
            } else if (Boolean.TRUE.equals(isPaid) && s.getStatus() != StatementStatus.PAID) {
                s.setStatus(StatementStatus.PAID);
                commonExpenseStatementRepository.save(s);
            }

            boolean hasPayments = commonExpenseAllocationRepository.hasAnyPaymentForStatement(s.getId());

            CommonExpenseStatementDTO dto = CommonExpenseStatementMapper.toDTO(s);
            dto.setHasPayments(hasPayments);
            return dto;

        }).toList();
    }

    public List<CommonExpenseStatementDTO> getActiveStatementsByBuildingDTO(Integer buildingId) {
        return commonExpenseStatementRepository.findActiveByBuildingId(buildingId)
                .stream()
                .map(s -> {
                    boolean hasPayments = commonExpenseAllocationRepository.hasAnyPaymentForStatement(s.getId());
                    CommonExpenseStatementDTO dto = CommonExpenseStatementMapper.toDTO(s);
                    dto.setHasPayments(hasPayments);
                    return dto;
                }).toList();
    }

    @Transactional
    public void delete(Integer id) {
        if (commonExpenseAllocationRepository.hasAnyPaymentForStatement(id)) {
            throw new IllegalStateException("Δεν επιτρέπεται επεξεργασία/διαγραφή μετά από πληρωμή.");
        }

        CommonExpenseStatement statement = commonExpenseStatementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Δεν βρέθηκε statement με ID: " + id));

        boolean hasAllocations = statement.getItems().stream()
                .anyMatch(item -> item.getAllocations() != null && !item.getAllocations().isEmpty());

        if (statement.getStatus() == StatementStatus.DRAFT && !hasAllocations) {
            commonExpenseStatementRepository.delete(statement);
            System.out.println("Hard delete statement id=" + id);
        } else {
            // Hibernate θα κάνει soft delete μόνο του
            commonExpenseStatementRepository.delete(statement);
            System.out.println("Soft delete (SQLDelete) statement id=" + id);
        }
    }



    public String generateNextCode(Integer buildingId) {
        // Βρίσκουμε το max sequence του συγκεκριμένου building
        Integer maxSeq = commonExpenseStatementRepository.findMaxSequenceByBuilding(buildingId);
        int nextSeq = (maxSeq == null) ? 1 : maxSeq + 1;

        // Παίρνουμε την ημερομηνία σε μορφή YYYYMMDD
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        // Building id padded σε 6 ψηφία
        String buildingPart = String.format("%06d", buildingId);

        // Sequence padded σε 6 ψηφία
        String seqPart = String.format("%06d", nextSeq);

        // Τελικός κωδικός
        return String.format("%s-%s-%s", datePart, buildingPart, seqPart);
    }

    public CommonExpenseStatementDTO updateStatement(Integer id, CommonExpenseStatementDTO dto) {
        if (commonExpenseAllocationRepository.hasAnyPaymentForStatement(id)) {
            throw new IllegalStateException("Δεν επιτρέπεται επεξεργασία/διαγραφή μετά από πληρωμή.");
        }

        CommonExpenseStatement entity = commonExpenseStatementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε statement με id " + id));

        entity.setCode(dto.getCode());
        entity.setType(dto.getType());
        entity.setMonth(dto.getMonth());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setDiscountPercent(dto.getDiscountPercent());
        entity.setTaxPercent(dto.getTaxPercent());
        entity.setDescription(dto.getDescription());

        // Καθαρίζουμε τα items και ξαναβάζουμε
        entity.getItems().clear();
        dto.getItems().forEach(itemDto -> {
            CommonExpenseItem item = new CommonExpenseItem();
            item.setCategory(ExpenseCategory.valueOf(itemDto.getCategory()));
            item.setDescriptionItem(itemDto.getDescriptionItem());
            item.setPrice(itemDto.getPrice());
            item.setStatement(entity);
            entity.getItems().add(item);
        });

        // Υπολογισμός συνόλων
        BigDecimal subTotal = entity.getItems().stream()
                .map(i -> bd(i.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountPercent = bd(entity.getDiscountPercent());
        BigDecimal taxPercent = bd(entity.getTaxPercent());

        BigDecimal discount = subTotal
                .multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal taxed = subTotal.subtract(discount)
                .multiply(taxPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal total = subTotal.subtract(discount).add(taxed);

        entity.setSubTotal(s2(subTotal));
        entity.setTotal(s2(total));


        // αποθήκευση
        CommonExpenseStatement saved = commonExpenseStatementRepository.save(entity);

        // Επιστρέφουμε DTO
        return CommonExpenseStatementMapper.toDTO(saved);
    }



}
