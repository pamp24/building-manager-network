package com.buildingmanager.notification;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationDTO {
    private Integer id;
    private String type;
    private String message;
    private String payload;
    private LocalDateTime createdAt;
    private boolean read;
}

