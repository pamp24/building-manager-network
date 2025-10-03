package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import com.buildingmanager.commonExpenseAllocation.PaymentMethod;
import com.buildingmanager.commonExpenseItem.CommonExpenseItem;
import com.buildingmanager.commonExpenseItem.ExpenseCategory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonExpenseStatementService {

    private final CommonExpenseStatementRepository commonExpenseStatementRepository;
    private final ApartmentRepository apartmentRepository;
    private final CommonExpenseAllocationRepository commonExpenseAllocationRepository;


    @Transactional
    public CommonExpenseStatement createAndSend(CommonExpenseStatement statement) {
        // 1. Παίρνουμε το buildingId από το statement
        Integer buildingId = statement.getBuilding().getId();

        // 2. Υπολογισμός συνολικών ποσών (subtotal, discount, tax, total)
        double subTotal = statement.getItems().stream()
                .mapToDouble(i -> (i.getPrice() == null ? 0.0 : i.getPrice()))
                .sum();

        double discount = (subTotal * (statement.getDiscountPercent() == null ? 0 : statement.getDiscountPercent())) / 100;
        double taxed = ((subTotal - discount) * (statement.getTaxPercent() == null ? 0 : statement.getTaxPercent())) / 100;
        double total = subTotal - discount + taxed;

        statement.setSubTotal(subTotal);
        statement.setTotal(total);

        // 3. Ορισμός sequenceNumber ανά κτίριο
        Integer maxSeq = commonExpenseStatementRepository.findMaxSequenceByBuilding(buildingId);
        int nextSeq = (maxSeq == null) ? 1 : maxSeq + 1;
        statement.setSequenceNumber(nextSeq);

        // 4. Σύνδεση των items με το statement (reverse relation)
        statement.getItems().forEach(i -> i.setStatement(statement));

        // 5. Αποθήκευση statement και items
        CommonExpenseStatement saved = commonExpenseStatementRepository.save(statement);

        // 6. Φέρνουμε όλα τα διαμερίσματα της πολυκατοικίας
        List<Apartment> apartments = apartmentRepository.findAllByBuilding_Id(buildingId);

        //7. Προσθέτουμε status
        if (statement.getStatus() == null) {
            statement.setStatus(StatementStatus.ISSUED);
        }

        // 8. Για κάθε item → μοιράζουμε το ποσό στα διαμερίσματα με βάση τα χιλιοστά
        for (CommonExpenseItem item : saved.getItems()) {
            double itemTotal = (item.getPrice() == null ? 0.0 : item.getPrice());

            for (Apartment apt : apartments) {
                double share = 0;

                switch (item.getCategory()) {
                    case COMMON -> share = (apt.getCommonPercent() / 1000.0) * itemTotal;
                    case ELEVATOR -> share = (apt.getElevatorPercent() / 1000.0) * itemTotal;
                    case HEATING -> share = (apt.getHeatingPercent() / 1000.0) * itemTotal;
                    case EQUAL, OTHER, SPECIAL, OWNERS, BOILER -> share = itemTotal / apartments.size();
                }

                CommonExpenseAllocation allocation = CommonExpenseAllocation.builder()
                        .statement(saved)
                        .item(item)
                        .apartment(apt)
                        .commonPercent(apt.getCommonPercent())
                        .elevatorPercent(apt.getElevatorPercent())
                        .heatingPercent(apt.getHeatingPercent())
                        .amount(share)
                        .isPaid(false)
                        .build();

                commonExpenseAllocationRepository.save(allocation);
            }
        }


        // 8. Επιστροφή του saved statement
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

    @Transactional
    public void delete(Integer id) {
        CommonExpenseStatement statement = commonExpenseStatementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Δεν βρέθηκε statement με ID: " + id));

        boolean hasAllocations = statement.getItems().stream()
                .anyMatch(item -> item.getAllocations() != null && !item.getAllocations().isEmpty());

        //Hard delete μόνο αν είναι DRAFT & δεν έχει allocations
        if ("DRAFT".equals(statement.getStatus()) && !hasAllocations) {
            commonExpenseStatementRepository.delete(statement);
            return;
        }

        //Soft delete σε όλα τα υπόλοιπα
        statement.setActive(false);
        statement.getItems().forEach(item -> {
            item.setActive(false);
            if (item.getAllocations() != null) {
                item.getAllocations().forEach(alloc -> alloc.setActive(false));
            }
        });

        commonExpenseStatementRepository.save(statement);
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
    @Transactional
    public void markAsPaid(Integer allocationId, String paymentMethod) {
        CommonExpenseAllocation allocation = commonExpenseAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        allocation.setIsPaid(true);
        allocation.setPaymentMethod(PaymentMethod.valueOf(paymentMethod));
        allocation.setPaidDate(LocalDateTime.now());
        commonExpenseAllocationRepository.save(allocation);

        // Έλεγχος αν όλα έχουν πληρωθεί
        CommonExpenseStatement statement = allocation.getStatement();
        boolean allPaid = commonExpenseAllocationRepository
                .findAllByStatement_Id(statement.getId())
                .stream()
                .allMatch(CommonExpenseAllocation::getIsPaid);

        if (allPaid && Boolean.FALSE.equals(statement.getIsPaid())) {
            statement.setIsPaid(true);
            commonExpenseStatementRepository.save(statement);
        }
    }

    public List<CommonExpenseStatement> getStatementsByBuilding(Long buildingId) {
        return commonExpenseStatementRepository.findByBuildingId(buildingId);
    }

    public CommonExpenseStatementDTO updateStatement(Integer id, CommonExpenseStatementDTO dto) {
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
