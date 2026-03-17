package com.buildingmanager.pmDashboard;

import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pm/dashboard")
@RequiredArgsConstructor
public class PmDashboardController {

    private final PmDashboardService pmDashboardService;

    @GetMapping
    public ResponseEntity<PmDashboardDTO> getDashboard(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(pmDashboardService.getDashboard(user));
    }

    @GetMapping("/financial-chart")
    public ResponseEntity<PmFinancialChartDTO> getFinancialChart(
            Authentication authentication,
            @RequestParam(defaultValue = "month") String period
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(pmDashboardService.getFinancialChart(user, period));
    }

    @GetMapping("/expense-collection-rate")
    public ResponseEntity<PmExpenseCollectionRateDTO> getExpenseCollectionRate(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(pmDashboardService.getExpenseCollectionRate(user));
    }

    @GetMapping("/attention-buildings")
    public ResponseEntity<List<PmAttentionBuildingDTO>> getAttentionBuildings(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(pmDashboardService.getAttentionBuildings(user));
    }

    @GetMapping("/membership-stats")
    public ResponseEntity<PmMembershipStatsDTO> getMembershipStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(pmDashboardService.getMembershipStats(user));
    }
}
