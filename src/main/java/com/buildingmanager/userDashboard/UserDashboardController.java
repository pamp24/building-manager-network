package com.buildingmanager.userDashboard;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.commonExpenseItem.CommonExpenseItemDTO;
import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard/user")
public class UserDashboardController {

    private final ApartmentRepository apartmentRepository;

    @Autowired
    private UserDashboardService userDashboardService;


    @GetMapping
    public UserDashboardSummaryDTO getDashboard(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return userDashboardService.getDashboard(user.getId());
    }

    @GetMapping("/history")
    public List<UserStatementHistoryDTO> getHistory(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return userDashboardService.getHistory(user.getId());
    }

    @GetMapping("/last-statement-items")
    public ResponseEntity<List<CommonExpenseItemDTO>> getLastStatementItems(Authentication auth) {

        User user = (User) auth.getPrincipal();

        List<CommonExpenseItemDTO> items =
                userDashboardService.getLastStatementItems(user.getId());

        return ResponseEntity.ok(items);
    }
    @GetMapping("/chart")
    public ResponseEntity<ChartResponseDTO> getChart(
            Authentication auth,
            @RequestParam String type,      // "building" ή "apartment"
            @RequestParam String period     // "month" ή "year"
    ) {
        User user = (User) auth.getPrincipal();
        ChartResponseDTO chart = userDashboardService.getChartData(user.getId(), type, period);
        return ResponseEntity.ok(chart);
    }

    @GetMapping("/statement-mini-chart")
    public StatementMiniChartDTO getStatementMiniChart(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return userDashboardService.getStatementMiniChart(user.getId());
    }

    @GetMapping("/unpaid")
    public ResponseEntity<Double> getUnpaid(Authentication auth) {
        User user = (User) auth.getPrincipal();
        Double unpaid = userDashboardService.getUnpaidForUserApartment(user.getId());
        return ResponseEntity.ok(unpaid);
    }

    @GetMapping("/building-pending")
    public BuildingPendingDTO getPending(Authentication auth) {
        User user = (User) auth.getPrincipal();

        Apartment apt = apartmentRepository.findByOwnerOrResident(user, user)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        return userDashboardService.getBuildingPending(apt.getBuilding().getId());
    }

    @GetMapping("/statements")
    public ResponseEntity<List<UserStatementDTO>> getUserStatements(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(userDashboardService.getUserStatementTotals(user.getId()));
    }
}
