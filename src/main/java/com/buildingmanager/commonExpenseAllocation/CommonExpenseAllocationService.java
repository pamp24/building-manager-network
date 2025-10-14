package com.buildingmanager.commonExpenseAllocation;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonExpenseAllocationService {

    private final CommonExpenseAllocationRepository commonExpenseAllocationRepository;
    private final CommonExpenseStatementRepository commonExpenseStatementRepository;

    // Αναλυτικά τι χρωστάει κάθε apartment σε ένα statement
    public List<CommonExpenseAllocationDTO> getAllocationsForApartment(Integer statementId, Apartment apartment) {
        CommonExpenseStatement stubStatement = new CommonExpenseStatement();
        stubStatement.setId(statementId);

        return commonExpenseAllocationRepository.findByStatementAndApartment(stubStatement, apartment).stream()
                .map(CommonExpenseAllocationMapper::toDTO)
                .toList();
    }

    // Σύνολο ποσού που χρωστάει ένα apartment για ένα statement
    public double getTotalForApartment(Integer statementId, Apartment apartment) {
        CommonExpenseStatement stubStatement = new CommonExpenseStatement();
        stubStatement.setId(statementId);

        return commonExpenseAllocationRepository.findByStatementAndApartment(stubStatement, apartment).stream()
                .mapToDouble(CommonExpenseAllocation::getAmount)
                .sum();
    }
    @Transactional
    public CommonExpenseAllocation markAsPaid(Integer allocationId) {
        CommonExpenseAllocation allocation = commonExpenseAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        allocation.setIsPaid(true);
        allocation.setPaidDate(LocalDateTime.now());

        return commonExpenseAllocationRepository.save(allocation);
    }

    @Transactional
    public CommonExpenseAllocation markAsUnpaid(Integer allocationId) {
        CommonExpenseAllocation allocation = commonExpenseAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        allocation.setIsPaid(false);
        allocation.setPaidDate(null);

        return commonExpenseAllocationRepository.save(allocation);
    }
    @Transactional
    public void payAllocation(Integer allocationId, Double amount) {
        CommonExpenseAllocation allocation = commonExpenseAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Δεν βρέθηκε η κατανομή"));

        allocation.setPaidAmount(amount);
        allocation.setIsPaid(true);
        allocation.setPaidDate(LocalDateTime.now());
        commonExpenseAllocationRepository.save(allocation);

        //Έλεγχος αν όλα τα allocations του statement έχουν πληρωθεί
        CommonExpenseStatement statement = allocation.getStatement();
        boolean allPaid = statement.getAllocations().stream().allMatch(CommonExpenseAllocation::getIsPaid);

        if (allocation.getDueDate() != null && LocalDateTime.now().isAfter(allocation.getDueDate())) {
            allocation.setStatus("OVERDUE");
        } else if (allocation.getIsPaid()) {
            allocation.setStatus("PAID");
        } else {
            allocation.setStatus("PENDING");
        }
    }

}
