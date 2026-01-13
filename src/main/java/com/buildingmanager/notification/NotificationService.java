package com.buildingmanager.notification;

import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void create(User receiver, String type, String message, String payload) {
        Notification n = Notification.builder()
                .user(receiver)
                .type(type)
                .message(message)
                .payload(payload)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(n);
    }

    public List<NotificationDTO> getMy(Integer userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> new NotificationDTO(
                        n.getId(),
                        n.getType(),
                        n.getMessage(),
                        n.getPayload(),
                        n.getCreatedAt(),
                        n.isRead()
                ))
                .toList();
    }

    public void markAllRead(Integer userId) {
        List<Notification> list = notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        list.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(list);
    }
}


