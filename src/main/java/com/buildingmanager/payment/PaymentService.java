package com.buildingmanager.payment;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CommonExpenseStatementRepository commonExpenseStatementRepository;
    private final UserRepository userRepository;
    private final CommonExpenseAllocationRepository commonExpenseAllocationRepository;
    private final ApartmentRepository apartmentRepository;

    @Transactional
    public PaymentDTO createPayment(PaymentRequest req) {
        System.out.println("[DEBUG] Received PaymentRequest: " + req);

        if (req.getStatementId() == null) {
            throw new IllegalArgumentException("StatementId is required");
        }

        CommonExpenseStatement statement = commonExpenseStatementRepository.findById(req.getStatementId())
                .orElseThrow(() -> new IllegalArgumentException("Statement not found"));

        User user = null;
        Apartment apartment = null;

        if (req.getUserId() != null) {
            user = userRepository.findById(req.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }

        if (req.getApartmentId() != null) {
            apartment = apartmentRepository.findById(req.getApartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Apartment not found"));
        }

        PaymentMethod method;
        try {
            method = req.getPaymentMethod() != null
                    ? PaymentMethod.valueOf(req.getPaymentMethod().toUpperCase())
                    : PaymentMethod.CASH;
        } catch (IllegalArgumentException ex) {
            method = PaymentMethod.CASH;
        }

        // 🔎 Εύρεση υπαρχουσών κατανομών
        List<CommonExpenseAllocation> allocations = (user != null)
                ? commonExpenseAllocationRepository.findByStatementIdAndUserId(req.getStatementId(), req.getUserId())
                : commonExpenseAllocationRepository.findByStatementIdAndApartmentId(req.getStatementId(), req.getApartmentId());

        if (allocations.isEmpty()) {
            throw new IllegalArgumentException("No allocations found for this user/apartment/statement");
        }

        double totalOwed = allocations.stream().mapToDouble(a -> a.getAmount() != null ? a.getAmount() : 0.0).sum();
        double totalPaid = allocations.stream().mapToDouble(a -> a.getPaidAmount() != null ? a.getPaidAmount() : 0.0).sum();

        if (totalPaid >= totalOwed - 0.01) {
            throw new IllegalStateException("Αυτό το διαμέρισμα έχει ήδη εξοφληθεί πλήρως.");
        }
        if (totalPaid + req.getAmount() > totalOwed + 0.01) {
            throw new IllegalStateException("Το ποσό υπερβαίνει το οφειλόμενο υπόλοιπο.");
        }

        // 🧭 Εύρεση υπάρχουσας πληρωμής
        Optional<Payment> existingPaymentOpt = Optional.empty();

        if (user != null) {
            existingPaymentOpt = Optional.ofNullable(
                    paymentRepository.findTopByUser_IdAndStatement_IdOrderByPaymentDateDesc(
                            user.getId(), statement.getId())
            );
        } else if (apartment != null) {
            existingPaymentOpt = paymentRepository.findTopByApartment_IdAndStatement_IdOrderByPaymentDateDesc(
                    apartment.getId(), statement.getId());
        }

        Payment payment;
        if (existingPaymentOpt.isPresent()) {
            // 🔁 Ενημέρωση υπάρχουσας πληρωμής
            payment = existingPaymentOpt.get();
            System.out.println("🔁 Updating existing payment");

            payment.setAmount(payment.getAmount() + req.getAmount());
            payment.setPaymentDate(LocalDateTime.now());
            payment.setPaymentMethod(method);
            payment.setReferenceNumber(req.getReferenceNumber());

        } else {
            // ➕ Δημιουργία νέας πληρωμής
            System.out.println("➕ Creating new payment");

            payment = Payment.builder()
                    .user(user)
                    .apartment(apartment)
                    .statement(statement)
                    .amount(req.getAmount())
                    .paymentDate(LocalDateTime.now())
                    .paymentMethod(method)
                    .referenceNumber(req.getReferenceNumber())
                    .build();
        }

        // ✅ Πάντα συνδέουμε το apartment για ασφάλεια
        if (apartment != null) {
            payment.setApartment(apartment);
        }

        payment = paymentRepository.save(payment);

        // 🧮 Ενημέρωση κατανομών
        double remainingToAllocate = req.getAmount();
        for (CommonExpenseAllocation alloc : allocations) {
            if (remainingToAllocate <= 0) break;

            double currentPaid = Optional.ofNullable(alloc.getPaidAmount()).orElse(0.0);
            double totalAmount = Optional.ofNullable(alloc.getAmount()).orElse(0.0);
            double add = Math.min(remainingToAllocate, totalAmount - currentPaid);

            alloc.setPaidAmount(currentPaid + add);
            alloc.setPaidDate(LocalDateTime.now());
            alloc.setPaymentMethod(method);
            alloc.setIsPaid((alloc.getPaidAmount() + 0.01) >= totalAmount);
            alloc.setStatus(alloc.getIsPaid() ? "PAID" : (alloc.getPaidAmount() > 0 ? "PARTIALLY_PAID" : "UNPAID"));

            commonExpenseAllocationRepository.save(alloc);
            remainingToAllocate -= add;
        }

        // 🧾 Ενημέρωση statement
        List<CommonExpenseAllocation> allAllocations = commonExpenseAllocationRepository.findAllByStatement_Id(req.getStatementId());
        boolean allPaid = allAllocations.stream().allMatch(a -> Boolean.TRUE.equals(a.getIsPaid()));
        statement.setStatus(allPaid ? StatementStatus.PAID : StatementStatus.ISSUED);
        statement.setIsPaid(allPaid);
        commonExpenseStatementRepository.save(statement);

        // 🎁 Δημιουργία DTO
        String fullName;
        if (user != null) {
            fullName = user.getFullName();
        } else if (apartment != null) {
            fullName = apartment.getOwnerFirstName() + " " + apartment.getOwnerLastName();
        } else {
            fullName = "Μη συνδεδεμένος ένοικος";
        }

        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setReferenceNumber(payment.getReferenceNumber());
        dto.setUserId(user != null ? user.getId() : null);
        dto.setUserFullName(fullName);
        dto.setStatementId(statement.getId());

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

    public List<StatementUserPaymentDTO> getCurrentMonthPayments(Integer buildingId) {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(LocalTime.MAX);

        return paymentRepository.findUserPaymentsByBuildingAndCurrentMonth(buildingId, startOfMonth, endOfMonth);
    }

    private YearMonth parseIsoMonth(String month) {
        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException("Το πεδίο month είναι κενό ή null");
        }

        try {
            return YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception e1) {
            try {
                if (month.length() <= 3) {
                    String withYear = month + " " + YearMonth.now().getYear();
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("LLL yyyy", new Locale("el"));
                    return YearMonth.parse(withYear, fmt);
                }

                try {
                    DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("LLL yyyy", new Locale("el"));
                    return YearMonth.parse(month, shortFmt);
                } catch (Exception e2) {
                    DateTimeFormatter longFmt = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("el"));
                    return YearMonth.parse(month, longFmt);
                }
            } catch (Exception e3) {
                throw new IllegalArgumentException("Μη αναγνωρίσιμο format μήνα: " + month);
            }
        }
    }

    public List<StatementUserPaymentDTO> getPaymentsByBuildingAndMonth(Integer buildingId, String month) {
        YearMonth ym = parseIsoMonth(month);
        LocalDateTime startOfMonth = ym.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = ym.atEndOfMonth().atTime(LocalTime.MAX);

        List<Payment> payments = paymentRepository.findByBuildingIdAndMonth(
                Long.valueOf(buildingId), startOfMonth, endOfMonth);

        if (payments.isEmpty()) {
            System.out.println("Δεν βρέθηκαν πληρωμές για τον μήνα " + month + " και κτίριο " + buildingId);
            return Collections.emptyList();
        }

        return payments.stream()
                .map(this::convertToStatementUserPaymentDTO)
                .collect(Collectors.toList());
    }

    private StatementUserPaymentDTO convertToStatementUserPaymentDTO(Payment payment) {
        if (payment == null) return null;

        User user = payment.getUser();
        CommonExpenseStatement statement = payment.getStatement();

        return new StatementUserPaymentDTO(
                user != null ? user.getId() : null,
                user != null ? user.getFirstName() : "-",
                user != null ? user.getLastName() : "-",
                statement != null && statement.getBuilding() != null ? statement.getBuilding().getId() : null,
                statement != null ? statement.getCode() : "-",
                statement != null ? statement.getMonth() : "-",
                payment.getAmount(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentMethod(),
                "PAID"
        );
    }

    public List<StatementUserPaymentDTO> getUserPaymentsForLastStatement(Integer buildingId) {
        return paymentRepository.findUserPaymentsByLastStatement(buildingId);
    }
}
