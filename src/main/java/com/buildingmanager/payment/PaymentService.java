package com.buildingmanager.payment;

import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import com.buildingmanager.commonExpenseStatement.StatementStatus;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CommonExpenseStatementRepository commonExpenseStatementRepository;
    private final UserRepository userRepository;
    private final CommonExpenseAllocationRepository commonExpenseAllocationRepository;

    @Transactional
    public PaymentDTO createPayment(PaymentRequest req) {
        System.out.println("[DEBUG] Received PaymentRequest: " + req);
        System.out.println("=== DEBUG PaymentRequest ===");
        System.out.println("userId: " + req.getUserId());
        System.out.println("statementId: " + req.getStatementId());
        System.out.println("amount: " + req.getAmount());
        System.out.println("============================");

        // === Validation ===
        if (req.getStatementId() == null) {
            throw new IllegalArgumentException("StatementId is required");
        }

        CommonExpenseStatement statement = commonExpenseStatementRepository.findById(req.getStatementId())
                .orElseThrow(() -> new IllegalArgumentException("Statement not found"));

        User user = null;
        if (req.getUserId() != null) {
            user = userRepository.findById(req.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }

        // === Parse Payment Method safely ===
        PaymentMethod method;
        try {
            method = req.getPaymentMethod() != null
                    ? PaymentMethod.valueOf(req.getPaymentMethod().toUpperCase())
                    : PaymentMethod.CASH;
        } catch (IllegalArgumentException ex) {
            method = PaymentMethod.CASH; // default fallback
        }

        // === Εύρεση κατανομών (allocations) ===
        List<CommonExpenseAllocation> existingAllocations;
        if (user != null) {
            existingAllocations = commonExpenseAllocationRepository
                    .findByStatementIdAndUserId(req.getStatementId(), req.getUserId());
        } else {
            existingAllocations = commonExpenseAllocationRepository
                    .findByStatementIdAndUserNull(req.getStatementId());
        }

        if (existingAllocations.isEmpty()) {
            throw new IllegalArgumentException("No allocations found for this user/statement");
        }

        // === Έλεγχος πληρότητας / υπερπληρωμής ===
        double totalOwed = existingAllocations.stream()
                .mapToDouble(a -> a.getAmount() != null ? a.getAmount() : 0.0)
                .sum();

        double totalPaid = existingAllocations.stream()
                .mapToDouble(a -> a.getPaidAmount() != null ? a.getPaidAmount() : 0.0)
                .sum();

        if (totalPaid >= totalOwed - 0.01) {
            throw new IllegalStateException("Αυτό το διαμέρισμα έχει ήδη εξοφληθεί πλήρως.");
        }

        if (totalPaid + req.getAmount() > totalOwed + 0.01) {
            throw new IllegalStateException("Το ποσό υπερβαίνει το οφειλόμενο υπόλοιπο.");
        }

        // === Καταχώριση νέας πληρωμής ===
        Payment payment = Payment.builder()
                .user(user)
                .statement(statement)
                .amount(req.getAmount())
                .paymentDate(req.getPaymentDate() != null ? req.getPaymentDate() : LocalDateTime.now())
                .paymentMethod(method)
                .referenceNumber(req.getReferenceNumber())
                .build();

        paymentRepository.save(payment);
        System.out.println("💾 Payment saved successfully: " + payment.getId());

        // === Ενημέρωση allocations ===
        double remainingToAllocate = req.getAmount();

        for (CommonExpenseAllocation allocation : existingAllocations) {
            if (remainingToAllocate <= 0) break;

            double currentPaid = allocation.getPaidAmount() != null ? allocation.getPaidAmount() : 0.0;
            double totalAmount = allocation.getAmount() != null ? allocation.getAmount() : 0.0;

            // Υπολογίζουμε πόσο μένει να πληρωθεί για αυτό το allocation
            double remainingForThis = totalAmount - currentPaid;
            double add = Math.min(remainingToAllocate, remainingForThis);

            // Προσθέτουμε το νέο ποσό στο ήδη πληρωμένο
            allocation.setPaidAmount(currentPaid + add);
            allocation.setPaidDate(LocalDateTime.now());
            allocation.setPaymentMethod(method);

            // ✅ Κάνουμε σωστό update status
            if (allocation.getPaidAmount() >= totalAmount - 0.01) {
                allocation.setStatus("PAID");
                allocation.setIsPaid(true);
            } else if (allocation.getPaidAmount() > 0) {
                allocation.setStatus("PARTIALLY_PAID");
                allocation.setIsPaid(false);
            } else {
                allocation.setStatus("UNPAID");
                allocation.setIsPaid(false);
            }

            commonExpenseAllocationRepository.save(allocation);

            remainingToAllocate -= add;

            System.out.printf(
                    "🧾 Updated allocation: Apartment=%s, Paid=%.2f/%.2f, Status=%s%n",
                    allocation.getApartment().getNumber(),
                    allocation.getPaidAmount(),
                    totalAmount,
                    allocation.getStatus()
            );
        }

        // === Ενημέρωση κατάστασης statement ===
        List<CommonExpenseAllocation> allAllocations =
                commonExpenseAllocationRepository.findAllByStatement_Id(req.getStatementId());

        boolean allPaid = allAllocations.stream().allMatch(a -> Boolean.TRUE.equals(a.getIsPaid()));
        boolean somePaid = allAllocations.stream().anyMatch(a -> {
            Double paid = a.getPaidAmount();
            return paid != null && paid > 0 && !Boolean.TRUE.equals(a.getIsPaid());
        });

        if (allPaid) {
            statement.setStatus(StatementStatus.PAID);
            System.out.println("✅ All allocations paid. Statement marked as PAID.");
        } else {
            // Αν υπάρχει οποιαδήποτε πληρωμή αλλά όχι πλήρης εξόφληση → παραμένει ISSUED
            statement.setStatus(StatementStatus.ISSUED);
            System.out.println("⚠️ Statement still ISSUED (partial payments detected).");
        }

        commonExpenseStatementRepository.save(statement);

        // === Return DTO ===
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setReferenceNumber(payment.getReferenceNumber());
        dto.setUserId(user != null ? user.getId() : null);
        dto.setUserFullName(user != null ? user.getFullName() : "Μη συνδεδεμένος ένοικος");

        return dto;
    }


    public List<PaymentDTO> getPaymentsForStatement(Integer statementId, int size) {
        return paymentRepository.findPaymentsByStatementId(statementId, PageRequest.of(0, size));
    }

    public List<PaymentDTO> getRecentPaymentsByBuilding(Integer buildingId, int size) {
        return paymentRepository.findRecentByBuilding(buildingId, PageRequest.of(0, size));
    }

    public List<StatementUserPaymentDTO> getUserPaymentsForStatement(Integer statementId) {
        return paymentRepository.findUserPaymentsByStatement(statementId);
    }
    public CommonStatementSummaryDTO getBuildingSummary(Integer buildingId) {
        return paymentRepository.findBuildingSummary(buildingId);
    }

}
