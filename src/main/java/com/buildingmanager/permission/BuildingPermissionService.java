package com.buildingmanager.permission;

import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingPermissionService {

    private final BuildingMemberRepository buildingMemberRepository;
    private final UserBuildingPermissionRepository permissionRepository;

    public boolean canViewBuilding(User user, Integer buildingId) {
        String role = normalizeRole(user);

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

        return permissionRepository.findByUserId(user.getId())
                .stream()
                .map(permission -> permission.getBuilding().getId())
                .toList();
    }
}
