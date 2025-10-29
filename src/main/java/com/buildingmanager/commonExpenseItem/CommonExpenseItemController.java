package com.buildingmanager.commonExpenseItem;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
@CrossOrigin
public class CommonExpenseItemController {

    private final CommonExpenseItemService service;

    @GetMapping("/building/{buildingId}/current-month")
    public ResponseEntity<List<ExpenseCategorySummaryDTO>> getCurrentMonthTotals(@PathVariable Integer buildingId) {
        return ResponseEntity.ok(service.getCurrentMonthTotals(buildingId));
    }
    @GetMapping("/building/{buildingId}/summary")
    public ResponseEntity<List<ExpenseCategorySummaryDTO>> getSummary(
            @PathVariable Integer buildingId,
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(service.getTotals(buildingId, period));
    }
}
