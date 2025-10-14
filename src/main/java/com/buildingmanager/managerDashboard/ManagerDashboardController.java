package com.buildingmanager.managerDashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final ManagerDashboardService dashboardService;

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<ManagerDashboardDTO> getDashboardForBuilding(@PathVariable Integer buildingId) {
        return ResponseEntity.ok(dashboardService.getDashboardForBuilding(buildingId));
    }
}
