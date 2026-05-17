package com.buildingmanager.notification;

import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

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

    public void notifyAdmins(String type, String message, String payload) {
        List<User> admins = userRepository.findByRole_Name("Admin");

        List<Notification> notifications = admins.stream()
                .map(admin -> Notification.builder()
                        .user(admin)
                        .type(type)
                        .message(message)
                        .payload(payload)
                        .read(false)
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);
    }
}


