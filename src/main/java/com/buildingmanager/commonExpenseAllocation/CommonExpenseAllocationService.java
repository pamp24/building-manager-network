package com.buildingmanager.commonExpenseAllocation;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.math.RoundingMode;


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
    public BigDecimal getTotalForApartment(Integer statementId, Apartment apartment) {
        CommonExpenseStatement stub = new CommonExpenseStatement();
        stub.setId(statementId);

        return commonExpenseAllocationRepository.findByStatementAndApartment(stub, apartment).stream()
                .map(CommonExpenseAllocation::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
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

}
