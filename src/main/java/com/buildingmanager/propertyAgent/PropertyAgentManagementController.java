package com.buildingmanager.propertyAgent;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/property-agents")
@RequiredArgsConstructor
public class PropertyAgentManagementController {

    private final PropertyAgentManagementService propertyAgentManagementService;

    @GetMapping("/my-company")
    public ResponseEntity<List<PropertyAgentManagementResponse>> getMyCompanyAgents() {
        return ResponseEntity.ok(propertyAgentManagementService.getMyCompanyAgents());
    }

    @PutMapping("/{agentId}/buildings")
    public ResponseEntity<PropertyAgentManagementResponse> updateAgentBuildings(
            @PathVariable Integer agentId,
            @RequestBody UpdatePropertyAgentBuildingsRequest request
    ) {
        return ResponseEntity.ok(propertyAgentManagementService.updateAgentBuildings(agentId, request));
    }

    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> removeAgent(@PathVariable Integer agentId) {
        propertyAgentManagementService.removeAgent(agentId);
        return ResponseEntity.noContent().build();
    }
}