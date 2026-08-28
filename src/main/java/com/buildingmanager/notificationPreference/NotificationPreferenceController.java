package com.buildingmanager.notificationPreference;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<NotificationPreferenceDTO> getPreferences(Authentication connectedUser) {
        return ResponseEntity.ok(preferenceService.getPreferences(connectedUser));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferenceDTO> updatePreferences(
            @RequestBody NotificationPreferenceDTO dto,
            Authentication connectedUser
    ) {
        return ResponseEntity.ok(preferenceService.updatePreferences(dto, connectedUser));
    }
}
