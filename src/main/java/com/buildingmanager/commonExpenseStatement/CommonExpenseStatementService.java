package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import com.buildingmanager.payment.PaymentMethod;
import com.buildingmanager.commonExpenseItem.CommonExpenseItem;
import com.buildingmanager.commonExpenseItem.ExpenseCategory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // 4. Σύνδεση των items με το statement
        statement.getItems().forEach(i -> i.setStatement(statement));

        // 5. Αποθήκευση statement και items
        CommonExpenseStatement saved = commonExpenseStatementRepository.save(statement);

        // 6. Φέρνουμε όλα τα διαμερίσματα της πολυκατοικίας
        List<Apartment> apartments = apartmentRepository.findAllByBuilding_Id(buildingId);

        // 7. Προσθέτουμε status
        if (statement.getStatus() == null) {
            statement.setStatus(StatementStatus.ISSUED);
        }

        // 8. Δημιουργία allocations ανά item & apartment
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

                //Λογική “ποιος πληρώνει”
                if (apt.getResident() != null) {
                    if (item.getCategory() == ExpenseCategory.OWNERS) {
                        allocation.setUser(apt.getOwner()); // μόνο οι ιδιοκτήτες για OWNERS
                    } else {
                        allocation.setUser(apt.getResident()); // τα υπόλοιπα στον ένοικο
                    }
                } else {
                    allocation.setUser(apt.getOwner()); // αν δεν υπάρχει ένοικος → όλα στον ιδιοκτήτη
                }

                commonExpenseAllocationRepository.save(allocation);

                System.out.printf(
                        "🧾 Created allocation | Apartment=%s | Category=%s | User=%s | Amount=%.2f%n",
                        apt.getNumber(),
                        item.getCategory(),
                        allocation.getUser() != null ? allocation.getUser().getFullName() : "Χωρίς χρήστη",
                        share
                );
            }
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

    public List<CommonExpenseStatement> getStatementsByBuilding(Integer buildingId) {
        List<CommonExpenseStatement> statements = commonExpenseStatementRepository.findByBuildingId(buildingId);
        LocalDateTime now = LocalDateTime.now();

        statements.forEach(s -> {
            Boolean isPaid = s.getIsPaid() != null ? s.getIsPaid() : false;

            //Αν δεν έχει πληρωθεί και έχει λήξει → γίνεται EXPIRED
            if (!isPaid && s.getEndDate() != null
                    && s.getEndDate().isBefore(now)
                    && s.getStatus() == StatementStatus.ISSUED) {
                s.setStatus(StatementStatus.EXPIRED);
                commonExpenseStatementRepository.save(s);
            }

            //Αν έχει πληρωθεί → γίνεται PAID
            else if (Boolean.TRUE.equals(isPaid)
                    && s.getStatus() != StatementStatus.PAID) {
                s.setStatus(StatementStatus.PAID);
                commonExpenseStatementRepository.save(s);
            }
        });

        return statements;
    }

    public List<CommonExpenseStatement> getActiveStatementsByBuilding(Integer buildingId) {
        // Παίρνουμε μόνο τα statements με status ISSUED, PAID ή EXPIRED
        return commonExpenseStatementRepository.findActiveByBuildingId(buildingId);
    }

    @Transactional
    public void delete(Integer id) {
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

    public Map<String, Long> getStatementCounters(Integer buildingId) {
        YearMonth currentMonth = YearMonth.now();

        List<CommonExpenseStatement> statements = commonExpenseStatementRepository
                .findByBuildingId(buildingId);

        long issuedCount = statements.stream()
                .filter(s -> YearMonth.from(s.getStartDate()).equals(currentMonth))
                .count();

        long paidCount = statements.stream()
                .filter(CommonExpenseStatement::getIsPaid)
                .count();

        long pendingCount = statements.stream()
                .filter(s -> !s.getIsPaid() && (s.getEndDate() == null || !s.getEndDate().isBefore(LocalDate.now().atStartOfDay())))
                .count();

        long overdueCount = statements.stream()
                .filter(s -> !s.getIsPaid() && s.getEndDate() != null && s.getEndDate().isBefore(LocalDate.now().atStartOfDay()))
                .count();

        Map<String, Long> counters = new HashMap<>();
        counters.put("issuedCount", issuedCount);
        counters.put("paidCount", paidCount);
        counters.put("pendingCount", pendingCount);
        counters.put("overdueCount", overdueCount);

        return counters;
    }

}
