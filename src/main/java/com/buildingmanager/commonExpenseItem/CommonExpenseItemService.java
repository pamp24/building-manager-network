package com.buildingmanager.commonExpenseItem;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonExpenseItemService {

    private final CommonExpenseItemRepository repository;

    public List<ExpenseCategorySummaryDTO> getCurrentMonthTotals(Integer buildingId) {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(LocalTime.MAX);
        return repository.findCategoryTotals(buildingId, startOfMonth, endOfMonth);
    }

    public List<ExpenseCategorySummaryDTO> getTotals(Integer buildingId, String period) {
        LocalDate now = LocalDate.now();
        LocalDateTime start;
        LocalDateTime end;

        switch (period.toLowerCase()) {
            case "year":
                start = now.withDayOfYear(1).atStartOfDay();
                end = now.withMonth(12).withDayOfMonth(31).atTime(LocalTime.MAX);
                break;
            case "all":
                start = LocalDate.of(2000, 1, 1).atStartOfDay();
                end = LocalDateTime.now();
                break;
            case "month":
            default:
                start = now.withDayOfMonth(1).atStartOfDay();
                end = now.withDayOfMonth(now.lengthOfMonth()).atTime(LocalTime.MAX);
                break;
        }

        return repository.findCategoryTotals(buildingId, start, end);
    }

}
