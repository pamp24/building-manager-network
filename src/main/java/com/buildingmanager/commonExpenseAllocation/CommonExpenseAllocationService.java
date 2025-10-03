package com.buildingmanager.commonExpenseAllocation;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonExpenseAllocationService {

    private final CommonExpenseAllocationRepository allocationRepository;

    // Αναλυτικά τι χρωστάει κάθε apartment σε ένα statement
    public List<CommonExpenseAllocationDTO> getAllocationsForApartment(Integer statementId, Apartment apartment) {
        CommonExpenseStatement stubStatement = new CommonExpenseStatement();
        stubStatement.setId(statementId);

        return allocationRepository.findByStatementAndApartment(stubStatement, apartment).stream()
                .map(CommonExpenseAllocationMapper::toDTO)
                .toList();
    }

    // Σύνολο ποσού που χρωστάει ένα apartment για ένα statement
    public double getTotalForApartment(Integer statementId, Apartment apartment) {
        CommonExpenseStatement stubStatement = new CommonExpenseStatement();
        stubStatement.setId(statementId);

        return allocationRepository.findByStatementAndApartment(stubStatement, apartment).stream()
                .mapToDouble(CommonExpenseAllocation::getAmount)
                .sum();
    }
    @Transactional
    public CommonExpenseAllocation markAsPaid(Integer allocationId) {
        CommonExpenseAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        allocation.setIsPaid(true);
        allocation.setPaidDate(LocalDateTime.now());

        return allocationRepository.save(allocation);
    }

    @Transactional
    public CommonExpenseAllocation markAsUnpaid(Integer allocationId) {
        CommonExpenseAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        allocation.setIsPaid(false);
        allocation.setPaidDate(null);

        return allocationRepository.save(allocation);
    }
}
