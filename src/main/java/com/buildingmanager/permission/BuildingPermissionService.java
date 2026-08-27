package com.buildingmanager.permission;

import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingPermissionService {

    private final BuildingMemberRepository buildingMemberRepository;
    private final UserBuildingPermissionRepository permissionRepository;


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
}
