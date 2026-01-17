package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import com.buildingmanager.notification.NotificationService;
import com.buildingmanager.commonExpenseItem.CommonExpenseItem;
import com.buildingmanager.commonExpenseItem.ExpenseCategory;

import com.buildingmanager.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommonExpenseStatementService {

    private final CommonExpenseStatementRepository commonExpenseStatementRepository;
    private final ApartmentRepository apartmentRepository;
    private final CommonExpenseAllocationRepository commonExpenseAllocationRepository;
    private final NotificationService notificationService;
    private final BuildingRepository buildingRepository;
    private final ObjectMapper objectMapper;


    @Transactional
    public CommonExpenseStatement createAndSend(CommonExpenseStatement statement) {

        // 1) buildingId
        Integer buildingId = statement.getBuilding().getId();

        // 2) totals
        double subTotal = statement.getItems().stream()
                .mapToDouble(i -> (i.getPrice() == null ? 0.0 : i.getPrice()))
                .sum();

        double discount = (subTotal * (statement.getDiscountPercent() == null ? 0 : statement.getDiscountPercent())) / 100;
        double taxed = ((subTotal - discount) * (statement.getTaxPercent() == null ? 0 : statement.getTaxPercent())) / 100;
        double total = subTotal - discount + taxed;

        statement.setSubTotal(subTotal);
        statement.setTotal(total);

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
            double itemTotal = (item.getPrice() == null ? 0.0 : item.getPrice());

            for (Apartment apt : apartments) {
                double share = switch (item.getCategory()) {
                    case COMMON -> (apt.getCommonPercent() / 1000.0) * itemTotal;
                    case ELEVATOR -> (apt.getElevatorPercent() / 1000.0) * itemTotal;
                    case HEATING -> (apt.getHeatingPercent() / 1000.0) * itemTotal;
                    case EQUAL, OTHER, SPECIAL, OWNERS, BOILER -> itemTotal / apartments.size();
                };

                CommonExpenseAllocation allocation = CommonExpenseAllocation.builder()
                        .statement(saved)
                        .item(item)
                        .apartment(apt)
                        .commonPercent(apt.getCommonPercent())
                        .elevatorPercent(apt.getElevatorPercent())
                        .heatingPercent(apt.getHeatingPercent())
                        .amount(share)
                        .isPaid(false)
                        .status("UNPAID")
                        .build();

                // ποιος πληρώνει
                if (apt.getResident() != null) {
                    if (item.getCategory() == ExpenseCategory.OWNERS) {
                        allocation.setUser(apt.getOwner());
                    } else {
                        allocation.setUser(apt.getResident());
                    }
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
        double subTotal = statement.getItems().stream()
                .mapToDouble(i -> (i.getPrice() == null ? 0.0 : i.getPrice()))
                .sum();

        double discount = (subTotal * (statement.getDiscountPercent() == null ? 0 : statement.getDiscountPercent())) / 100;
        double taxed = ((subTotal - discount) * (statement.getTaxPercent() == null ? 0 : statement.getTaxPercent())) / 100;
        double total = subTotal - discount + taxed;

        statement.setSubTotal(subTotal);
        statement.setTotal(total);

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
        double subTotal = entity.getItems().stream()
                .mapToDouble(CommonExpenseItem::getPrice)
                .sum();
        double discount = (subTotal * entity.getDiscountPercent()) / 100;
        double taxed = ((subTotal - discount) * entity.getTaxPercent()) / 100;
        double total = subTotal - discount + taxed;

        entity.setSubTotal(subTotal);
        entity.setTotal(total);

        // αποθήκευση
        CommonExpenseStatement saved = commonExpenseStatementRepository.save(entity);

        // Επιστρέφουμε DTO
        return CommonExpenseStatementMapper.toDTO(saved);
    }


}
