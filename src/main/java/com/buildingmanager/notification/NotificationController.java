package com.buildingmanager.notification;

import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public ResponseEntity<List<NotificationDTO>> my(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(notificationService.getMy(user.getId()));
    }

    @PostMapping("/my/read-all")
    public ResponseEntity<Void> readAll(Authentication auth) {
        User user = (User) auth.getPrincipal();
        notificationService.markAllRead(user.getId());
        return ResponseEntity.noContent().build();
    }
}

