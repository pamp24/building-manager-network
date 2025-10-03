package com.buildingmanager.commonExpenseAllocation;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expenses/allocations")
@RequiredArgsConstructor
public class CommonExpenseAllocationController {

    private final CommonExpenseAllocationService allocationService;
    private final CommonExpenseStatementRepository statementRepository;
    private final ApartmentRepository apartmentRepository;
    private final CommonExpenseAllocationService service;

    /**
     * 🔹 Επιστρέφει όλα τα allocations για συγκεκριμένο statement και apartment
     */
    @GetMapping("/statements/{statementId}/apartments/{apartmentId}")
    public ResponseEntity<List<CommonExpenseAllocationDTO>> getAllocationsForApartment(
            @PathVariable Integer statementId,
            @PathVariable Integer apartmentId
    ) {
        CommonExpenseStatement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new RuntimeException("Statement not found"));

        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        return ResponseEntity.ok(allocationService.getAllocationsForApartment(statementId, apartment));
    }

    /**
     * 🔹 Επιστρέφει το συνολικό ποσό που χρωστάει ένα apartment για ένα statement
     */
    @GetMapping("/statements/{statementId}/apartments/{apartmentId}/total")
    public ResponseEntity<Double> getTotalForApartment(
            @PathVariable Integer statementId,
            @PathVariable Integer apartmentId
    ) {
        CommonExpenseStatement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new RuntimeException("Statement not found"));

        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        return ResponseEntity.ok(allocationService.getTotalForApartment(statementId, apartment));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<CommonExpenseAllocation> markAsPaid(@PathVariable Integer id) {
        return ResponseEntity.ok(service.markAsPaid(id));
    }
    @PatchMapping("/{id}/unpay")
    public ResponseEntity<CommonExpenseAllocation> markAsUnpaid(@PathVariable Integer id) {
        return ResponseEntity.ok(service.markAsUnpaid(id));
    }
}
