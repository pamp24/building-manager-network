package com.buildingmanager.buildingNotificationSettings;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/buildings/{buildingId}/notification-settings")
@RequiredArgsConstructor
public class BuildingNotificationSettingsController {

    private final BuildingNotificationSettingsService settingsService;

    @GetMapping
    public ResponseEntity<BuildingNotificationSettingsDTO> getSettings(@PathVariable Integer buildingId) {
        return ResponseEntity.ok(settingsService.getSettings(buildingId));
    }

    @PutMapping
    public ResponseEntity<BuildingNotificationSettingsDTO> updateSettings(
            @PathVariable Integer buildingId,
            @RequestBody BuildingNotificationSettingsDTO dto
    ) {
        return ResponseEntity.ok(settingsService.updateSettings(buildingId, dto));
    }
}
