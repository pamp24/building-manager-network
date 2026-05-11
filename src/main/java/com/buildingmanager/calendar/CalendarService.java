package com.buildingmanager.calendar;

import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.notification.NotificationService;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.permission.UserBuildingPermissionRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository repository;
    private final CalendarMapper mapper;
    private final UserRepository userRepository;
    private final BuildingPermissionService buildingPermissionService;
    private final NotificationService notificationService;
    private final UserBuildingPermissionRepository userBuildingPermissionRepository;
    private final BuildingMemberRepository buildingMemberRepository;

    public List<CalendarDTO> getByBuilding(Integer buildingId, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!buildingPermissionService.canViewBuilding(user, buildingId)) {
            return List.of();
        }

        return repository.findByBuildingPinnedFirst(buildingId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    public CalendarDTO create(CalendarDTO dto, User currentUser) {
        Integer buildingId = dto.getBuildingId();

        System.out.println("CALENDAR CREATE USER ID = " + currentUser.getId());
        System.out.println("CALENDAR CREATE ROLE = " + currentUser.getRole().getName());
        System.out.println("CALENDAR CREATE BUILDING ID = " + buildingId);

        if (!buildingPermissionService.canManageBuilding(currentUser, buildingId)) {
            throw new AccessDeniedException("Δεν έχεις δικαίωμα δημιουργίας event σε αυτή την πολυκατοικία");
        }

        Calendar entity = mapper.toEntity(dto);
        entity.setActive(true);

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

        existing.setPinned(pinned);

        if (pinned) {
            unpinAllInBuilding(buildingId);
        }

        Calendar saved = repository.save(existing);
        return mapper.toDTO(saved);
    }

    private void unpinAllInBuilding(Integer buildingId) {
        List<Calendar> list = repository.findByBuildingIdAndActiveTrue(buildingId);
        list.forEach(e -> e.setPinned(false));
        repository.saveAll(list);
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

        if (dto.isPinned() && !existing.isPinned()) {
            unpinAllInBuilding(buildingId);
        }

        existing.setPinned(dto.isPinned());

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

        receivers.stream()
                .filter(user -> !user.getId().equals(creatorUserId))
                .forEach(user ->
                        notificationService.create(
                                user,
                                "CALENDAR_EVENT_CREATED",
                                message,
                                payload
                        )
                );
    }
}