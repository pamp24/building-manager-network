package com.buildingmanager.calendar;

import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.email.EmailService;
import com.buildingmanager.email.SmsService;
import com.buildingmanager.notification.NotificationService;
import com.buildingmanager.notificationPreference.NotificationPreferenceService;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.permission.UserBuildingPermissionRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CalendarService {

    private final CalendarRepository repository;
    private final CalendarMapper mapper;
    private final UserRepository userRepository;
    private final BuildingPermissionService buildingPermissionService;
    private final NotificationService notificationService;
    private final UserBuildingPermissionRepository userBuildingPermissionRepository;
    private final BuildingMemberRepository buildingMemberRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final EmailService emailService;
    private final SmsService smsService;

    public List<CalendarDTO> getByBuilding(Integer buildingId, Integer userId, boolean includeInactive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!buildingPermissionService.canViewBuilding(user, buildingId)) {
            return List.of();
        }

        // Αν κάποια ανακοίνωση έχει λήξει, απενεργοποίησέ την αυτόματα
        deactivateExpired(buildingId);

        boolean canManage = buildingPermissionService.canManageBuilding(user, buildingId);

        if (includeInactive && canManage) {
            // Ο διαχειριστής βλέπει και ενεργές και ανενεργές
            return repository.findByBuildingAllPinnedFirst(buildingId)
                    .stream()
                    .map(mapper::toDTO)
                    .toList();
        }

        // Κανονικοί χρήστες & dashboard → μόνο ενεργές ανακοινώσεις
        return repository.findByBuildingPinnedFirst(buildingId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    private void deactivateExpired(Integer buildingId) {
        LocalDateTime now = LocalDateTime.now();
        List<Calendar> expired = repository.findExpired(buildingId, now);
        if (expired.isEmpty()) {
            return;
        }
        for (Calendar event : expired) {
            event.setActive(false);
        }
        repository.saveAll(expired);
    }

    public CalendarDTO create(CalendarDTO dto, User currentUser) {
        Integer buildingId = dto.getBuildingId();

        log.debug("CALENDAR CREATE USER ID = {}, ROLE = {}, BUILDING ID = {}",
                currentUser.getId(), currentUser.getRole().getName(), buildingId);

        if (!buildingPermissionService.canCreateAnnouncement(currentUser, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα δημιουργίας ανακοίνωσης σε αυτή την πολυκατοικία");
        }

        Calendar entity = mapper.toEntity(dto);
        entity.setActive(true);
        entity.setCreatedByUser(currentUser);

        if (entity.isPinned()) {
            unpinAllInBuilding(entity.getBuilding().getId());
        }

        Calendar saved = repository.save(entity);

        notifyUsersForNewCalendarEvent(saved, currentUser.getId());

        return mapper.toDTO(saved);
    }

    public CalendarDTO pin(Integer id, boolean pinned, User currentUser) {
        Calendar existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Integer buildingId = existing.getBuilding().getId();

        if (!buildingPermissionService.canManageBuilding(currentUser, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα διαχείρισης calendar για αυτή την πολυκατοικία");
        }

        if (pinned) {
            unpinAllInBuilding(buildingId);
        }

        existing.setPinned(pinned);

        Calendar saved = repository.save(existing);
        return mapper.toDTO(saved);
    }

    private void unpinAllInBuilding(Integer buildingId) {
        repository.unpinAll(buildingId);
    }

    public void delete(Integer id, User currentUser) {
        Calendar existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Integer buildingId = existing.getBuilding().getId();

        if (!buildingPermissionService.canManageBuilding(currentUser, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα διαχείρισης calendar για αυτή την πολυκατοικία");
        }

        existing.setActive(false);
        existing.setPinned(false);
        repository.save(existing);
    }

    public CalendarDTO update(Integer id, CalendarDTO dto, User currentUser) {
        Calendar existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Integer buildingId = existing.getBuilding().getId();

        if (!buildingPermissionService.canManageBuilding(currentUser, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα διαχείρισης calendar για αυτή την πολυκατοικία");
        }

        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setColorPrimary(dto.getColorPrimary());

        Calendar updated = repository.save(existing);
        return mapper.toDTO(updated);
    }
    private void notifyUsersForNewCalendarEvent(Calendar event, Integer creatorUserId) {
        Integer buildingId = event.getBuilding().getId();

        Set<User> receivers = new HashSet<>();

        userBuildingPermissionRepository.findByBuilding_Id(buildingId)
                .forEach(permission -> {
                    if (permission.getUser() != null) {
                        receivers.add(permission.getUser());
                    }
                });

        buildingMemberRepository.findByBuilding_Id(buildingId)
                .forEach(member -> {
                    if (member.getUser() != null) {
                        receivers.add(member.getUser());
                    }
                });

        String message = "Νέο γεγονός στο ημερολόγιο: " + event.getTitle();

        String payload = """
        {
          "calendarEventId": %d,
          "buildingId": %d
        }
        """.formatted(
                event.getId(),
                buildingId
        );

        List<User> targets = receivers.stream()
                .filter(user -> !user.getId().equals(creatorUserId))
                .toList();

        targets.forEach(user -> {
            var prefs = notificationPreferenceService.getPreferencesForUser(user.getId());

            // In-app ειδοποίηση (μόνο αν είναι ενεργοποιημένη για τον συγκεκριμένο χρήστη)
            if (Boolean.TRUE.equals(prefs.getAppForNewAnnouncement())) {
                notificationService.create(
                        user,
                        "CALENDAR_EVENT_CREATED",
                        message,
                        payload
                );
            }

            // Email (μόνο αν είναι ενεργοποιημένη για τον συγκεκριμένο χρήστη)
            if (Boolean.TRUE.equals(prefs.getEmailForNewAnnouncement()) && user.getEmail() != null) {
                try {
                    emailService.sendNotificationEmail(
                            user.getEmail(),
                            user.getFullName(),
                            "Νέα ανακοίνωση στην πολυκατοικία",
                            message
                    );
                } catch (MessagingException e) {
                    // Μην αποτυγχάνει η δημιουργία ανακοίνωσης αν αποτύχει ένα email
                }
            }

            // SMS (stub — καταγράφεται στο log, χρειάζεται provider για πραγματική αποστολή)
            if (Boolean.TRUE.equals(prefs.getSmsForNewAnnouncement()) && user.getPhoneNumber() != null) {
                smsService.sendSms(user.getPhoneNumber(), user.getFullName(), message);
            }
        });
    }
}