package com.buildingmanager.permission;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.buildingNotificationSettings.BuildingNotificationSettingsService;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingPermissionService {

    private final BuildingMemberRepository buildingMemberRepository;
    private final UserBuildingPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final BuildingNotificationSettingsService notificationSettingsService;


    public boolean canViewBuilding(User user, Integer buildingId) {
        String role = normalizeRole(user);

        log.debug("Checking building {}: role={}, joined={}, VIEW={}, MANAGE={}, FULL={}",
                buildingId, role,
                isJoinedMember(user, buildingId),
                hasPermission(user, buildingId, BuildingPermissionLevel.VIEW),
                hasPermission(user, buildingId, BuildingPermissionLevel.MANAGE),
                hasPermission(user, buildingId, BuildingPermissionLevel.FULL));
        if ("ADMIN".equals(role)) {
            return true;
        }

        return hasPermission(user, buildingId, BuildingPermissionLevel.VIEW)
                || hasPermission(user, buildingId, BuildingPermissionLevel.MANAGE)
                || hasPermission(user, buildingId, BuildingPermissionLevel.FULL)
                || isJoinedMember(user, buildingId);
    }

    public boolean canManageBuilding(User user, Integer buildingId) {
        String role = normalizeRole(user);

        if ("ADMIN".equals(role)) {
            return true;
        }

        boolean hasPermission = hasPermission(user, buildingId, BuildingPermissionLevel.MANAGE)
                || hasPermission(user, buildingId, BuildingPermissionLevel.FULL);

        if (hasPermission) {
            return true;
        }

        boolean isJoined = isJoinedMember(user, buildingId);

        return isJoined && (
                "BUILDINGMANAGER".equals(role)
                        || "BUILDING_MANAGER".equals(role)
                        || "PROPERTYMANAGER".equals(role)
                        || "PROPERTY_MANAGER".equals(role)
        );
    }

    public boolean canFullManageBuilding(User user, Integer buildingId) {
        String role = normalizeRole(user);

        if ("ADMIN".equals(role)) {
            return true;
        }

        boolean hasFullPermission = hasPermission(user, buildingId, BuildingPermissionLevel.FULL);

        if (hasFullPermission) {
            return true;
        }

        boolean isJoined = isJoinedMember(user, buildingId);

        return isJoined && (
                "PROPERTYMANAGER".equals(role)
                        || "PROPERTY_MANAGER".equals(role)
        );
    }

    private boolean hasPermission(User user, Integer buildingId, BuildingPermissionLevel level) {
        return permissionRepository.findByUserIdAndBuildingId(user.getId(), buildingId)
                .map(permission -> permission.getPermissionLevel() == level)
                .orElse(false);
    }

    private boolean isJoinedMember(User user, Integer buildingId) {
        return buildingMemberRepository.existsByUserIdAndBuildingIdAndStatus(
                user.getId(),
                buildingId,
                BuildingMemberStatus.JOINED
        );
    }

    private String normalizeRole(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            return "";
        }

        return user.getRole().getName().trim().toUpperCase().replace(" ", "_");
    }

    public List<Integer> getUserBuildingIds(User user) {
        String role = normalizeRole(user);

        if ("ADMIN".equals(role)) {
            return List.of();
        }

        List<Integer> ids = new ArrayList<>();

        permissionRepository.findByUserId(user.getId())
                .forEach(permission -> ids.add(permission.getBuilding().getId()));

        // Οι διαχειριστές αλλά και τα μέλη που έχουν κάνει join
        // βρίσκονται στο building_members. Χωρίς αυτό, οι ψηφοφορίες
        // τους δεν εμφανίζονται (π.χ. στο /polls).
        buildingMemberRepository.findByUser_Id(user.getId())
                .stream()
                .filter(member -> member.getStatus() == BuildingMemberStatus.JOINED)
                .forEach(member -> {
                    if (member.getBuilding() != null) {
                        ids.add(member.getBuilding().getId());
                    }
                });

        return ids.stream().distinct().toList();
    }

    /**
     * Επιστρέφει τα δικαιώματα του τρέχοντος χρήστη για συγκεκριμένη πολυκατοικία,
     * ώστε το frontend να εμφανίζει/κρύβει σχετικά κουμπιά (π.χ. "+ Νέα Ψηφοφορία").
     */
    public MemberPermissionDTO myPermissions(Integer buildingId, User user) {
        boolean manager = canManageBuilding(user, buildingId);
        return MemberPermissionDTO.builder()
                .userId(user.getId())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .isManager(manager)
                .canCreateAnnouncement(canCreateAnnouncement(user, buildingId))
                .canCreatePoll(canCreatePoll(user, buildingId))
                .build();
    }


    /**
     * Ένα μέλος μπορεί να δημιουργήσει ψηφοφορία αν είναι διαχειριστής της
     * πολυκατοικίας ή αν ο διαχειριστής έχει ενεργοποιήσει το δικαίωμα για όλα τα μέλη
     * (μέσω του διακόπτη membersCanCreatePoll).
     */
    public boolean canCreatePoll(User user, Integer buildingId) {
        if (canManageBuilding(user, buildingId)) {
            return true;
        }

        Boolean allowed = notificationSettingsService.getSettings(buildingId).getMembersCanCreatePoll();
        return Boolean.TRUE.equals(allowed);
    }

    /**
     * Ένα μέλος μπορεί να δημιουργήσει ανακοίνωση αν είναι διαχειριστής της
     * πολυκατοικίας ή αν ο διαχειριστής έχει ενεργοποιήσει το δικαίωμα για όλα τα μέλη
     * (μέσω του διακόπτη membersCanCreateAnnouncement).
     */
    public boolean canCreateAnnouncement(User user, Integer buildingId) {
        if (canManageBuilding(user, buildingId)) {
            return true;
        }

        Boolean allowed = notificationSettingsService.getSettings(buildingId).getMembersCanCreateAnnouncement();
        return Boolean.TRUE.equals(allowed);
    }

    /**
     * Λίστα μελών της πολυκατοικίας με τα δικαιώματα δημιουργίας
     * ανακοίνωσης/ψηφοφορίας. Μόνο για διαχειριστή της πολυκατοικίας.
     */
    public List<MemberPermissionDTO> listMemberPermissions(Integer buildingId, User actor) {
        if (!canManageBuilding(actor, buildingId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Δεν έχεις δικαίωμα διαχείρισης μελών σε αυτή την πολυκατοικία.");
        }

        Map<Integer, User> memberUsers = new LinkedHashMap<>();

        buildingMemberRepository.findByBuilding_Id(buildingId)
                .stream()
                .filter(m -> m.getUser() != null && m.getStatus() == BuildingMemberStatus.JOINED)
                .forEach(m -> memberUsers.putIfAbsent(m.getUser().getId(), m.getUser()));

        permissionRepository.findByBuilding_Id(buildingId)
                .forEach(p -> {
                    if (p.getUser() != null) {
                        memberUsers.putIfAbsent(p.getUser().getId(), p.getUser());
                    }
                });

        List<MemberPermissionDTO> result = new ArrayList<>();

        for (User u : memberUsers.values()) {
            boolean isManager = canManageBuilding(u, buildingId);

            var permission = permissionRepository.findByUserIdAndBuildingId(u.getId(), buildingId);

            result.add(MemberPermissionDTO.builder()
                    .userId(u.getId())
                    .fullName(u.getFullName())
                    .email(u.getEmail())
                    .role(u.getRole() != null ? u.getRole().getName() : null)
                    .isManager(isManager)
                    .canCreateAnnouncement(isManager
                            || permission.map(p -> Boolean.TRUE.equals(p.getCanCreateAnnouncement())).orElse(false))
                    .canCreatePoll(isManager
                            || permission.map(p -> Boolean.TRUE.equals(p.getCanCreatePoll())).orElse(false))
                    .build());
        }

        return result;
    }

    /**
     * Ενημέρωση δικαιωμάτων δημιουργίας μέλους. Μόνο για διαχειριστή της πολυκατοικίας.
     */
    public MemberPermissionDTO updateMemberPermission(Integer buildingId, Integer userId, MemberPermissionDTO dto, User actor) {
        if (!canManageBuilding(actor, buildingId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Δεν έχεις δικαίωμα διαχείρισης μελών σε αυτή την πολυκατοικία.");
        }

        User member = userRepository.findById(userId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Ο χρήστης δεν βρέθηκε."));

        boolean isManager = canManageBuilding(member, buildingId);

        if (isManager) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Ο διαχειριστής δεν μπορεί να αφαιρέσει δικαιώματα από τον ίδιο.");
        }

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Η πολυκατοικία δεν βρέθηκε."));

        UserBuildingPermission permission = permissionRepository
                .findByUserIdAndBuildingId(userId, buildingId)
                .orElseGet(() -> UserBuildingPermission.builder()
                        .user(member)
                        .building(building)
                        .permissionLevel(BuildingPermissionLevel.VIEW)
                        .canCreateAnnouncement(false)
                        .canCreatePoll(false)
                        .build());

        permission.setCanCreateAnnouncement(Boolean.TRUE.equals(dto.getCanCreateAnnouncement()));
        permission.setCanCreatePoll(Boolean.TRUE.equals(dto.getCanCreatePoll()));

        permissionRepository.save(permission);

        return MemberPermissionDTO.builder()
                .userId(member.getId())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .role(member.getRole() != null ? member.getRole().getName() : null)
                .isManager(isManager)
                .canCreateAnnouncement(permission.getCanCreateAnnouncement())
                .canCreatePoll(permission.getCanCreatePoll())
                .build();
    }
}
