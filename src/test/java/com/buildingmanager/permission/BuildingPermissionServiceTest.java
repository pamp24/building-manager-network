package com.buildingmanager.permission;

import com.buildingmanager.building.Building;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.role.Role;
import com.buildingmanager.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingPermissionServiceTest {

    private BuildingPermissionService service;

    @Mock
    private BuildingMemberRepository buildingMemberRepository;
    @Mock
    private UserBuildingPermissionRepository permissionRepository;

    private User adminUser;
    private User buildingManagerUser;
    private User propertyManagerUser;
    private User regularUser;
    private Building building;

    @BeforeEach
    void setUp() {
        service = new BuildingPermissionService(buildingMemberRepository, permissionRepository);

        building = new Building();
        building.setId(1);

        adminUser = createUserWithRole("Admin");
        buildingManagerUser = createUserWithRole("BuildingManager");
        propertyManagerUser = createUserWithRole("PropertyManager");
        regularUser = createUserWithRole("User");
    }

    private User createUserWithRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return User.builder()
                .id(1)
                .role(role)
                .build();
    }

    @Test
    void canViewBuilding_adminRole_returnsTrue() {
        boolean result = service.canViewBuilding(adminUser, 1);
        assertThat(result).isTrue();
    }

    @Test
    void canManageBuilding_adminRole_returnsTrue() {
        boolean result = service.canManageBuilding(adminUser, 1);
        assertThat(result).isTrue();
    }

    @Test
    void canFullManageBuilding_adminRole_returnsTrue() {
        boolean result = service.canFullManageBuilding(adminUser, 1);
        assertThat(result).isTrue();
    }

    @Test
    void canViewBuilding_hasViewPermission_returnsTrue() {
        UserBuildingPermission perm = UserBuildingPermission.builder()
                .user(regularUser)
                .building(building)
                .permissionLevel(BuildingPermissionLevel.VIEW)
                .build();
        when(permissionRepository.findByUserIdAndBuildingId(anyInt(), anyInt()))
                .thenReturn(Optional.of(perm));

        boolean result = service.canViewBuilding(regularUser, 1);
        assertThat(result).isTrue();
    }

    @Test
    void canManageBuilding_hasManagePermission_returnsTrue() {
        UserBuildingPermission perm = UserBuildingPermission.builder()
                .user(regularUser)
                .building(building)
                .permissionLevel(BuildingPermissionLevel.MANAGE)
                .build();
        when(permissionRepository.findByUserIdAndBuildingId(anyInt(), anyInt()))
                .thenReturn(Optional.of(perm));

        boolean result = service.canManageBuilding(regularUser, 1);
        assertThat(result).isTrue();
    }

    @Test
    void canManageBuilding_hasFullPermission_returnsTrue() {
        UserBuildingPermission perm = UserBuildingPermission.builder()
                .user(regularUser)
                .building(building)
                .permissionLevel(BuildingPermissionLevel.FULL)
                .build();
        when(permissionRepository.findByUserIdAndBuildingId(anyInt(), anyInt()))
                .thenReturn(Optional.of(perm));

        boolean result = service.canManageBuilding(regularUser, 1);
        assertThat(result).isTrue();
    }

    @Test
    void canManageBuilding_buildingManagerAndJoined_returnsTrue() {
        when(buildingMemberRepository.existsByUserIdAndBuildingIdAndStatus(
                anyInt(), anyInt(), org.mockito.ArgumentMatchers.eq(BuildingMemberStatus.JOINED)))
                .thenReturn(true);

        boolean result = service.canManageBuilding(buildingManagerUser, 1);
        assertThat(result).isTrue();
    }

    @Test
    void canManageBuilding_propertyManagerAndJoined_returnsTrue() {
        when(buildingMemberRepository.existsByUserIdAndBuildingIdAndStatus(
                anyInt(), anyInt(), org.mockito.ArgumentMatchers.eq(BuildingMemberStatus.JOINED)))
                .thenReturn(true);

        boolean result = service.canManageBuilding(propertyManagerUser, 1);
        assertThat(result).isTrue();
    }

    @Test
    void canFullManageBuilding_propertyManagerAndJoined_returnsTrue() {
        when(buildingMemberRepository.existsByUserIdAndBuildingIdAndStatus(
                anyInt(), anyInt(), org.mockito.ArgumentMatchers.eq(BuildingMemberStatus.JOINED)))
                .thenReturn(true);

        boolean result = service.canFullManageBuilding(propertyManagerUser, 1);
        assertThat(result).isTrue();
    }

    @Test
    void canFullManageBuilding_buildingManagerAndJoined_returnsFalse() {
        when(buildingMemberRepository.existsByUserIdAndBuildingIdAndStatus(
                anyInt(), anyInt(), org.mockito.ArgumentMatchers.eq(BuildingMemberStatus.JOINED)))
                .thenReturn(true);

        boolean result = service.canFullManageBuilding(buildingManagerUser, 1);
        assertThat(result).isFalse();
    }

    @Test
    void canViewBuilding_joinedMemberWithoutPermission_returnsTrue() {
        when(buildingMemberRepository.existsByUserIdAndBuildingIdAndStatus(
                anyInt(), anyInt(), org.mockito.ArgumentMatchers.eq(BuildingMemberStatus.JOINED)))
                .thenReturn(true);

        boolean result = service.canViewBuilding(regularUser, 1);
        assertThat(result).isTrue();
    }

    @Test
    void canViewBuilding_notJoinedNoPermission_returnsFalse() {
        when(buildingMemberRepository.existsByUserIdAndBuildingIdAndStatus(
                anyInt(), anyInt(), org.mockito.ArgumentMatchers.eq(BuildingMemberStatus.JOINED)))
                .thenReturn(false);

        boolean result = service.canViewBuilding(regularUser, 1);
        assertThat(result).isFalse();
    }

    @Test
    void canManageBuilding_notJoinedNoPermission_returnsFalse() {
        boolean result = service.canManageBuilding(regularUser, 1);
        assertThat(result).isFalse();
    }

    @Test
    void canFullManageBuilding_notJoinedNoPermission_returnsFalse() {
        boolean result = service.canFullManageBuilding(regularUser, 1);
        assertThat(result).isFalse();
    }

    @Test
    void normalizeRole_trimsAndUppercases() {
        User user = createUserWithRole("  building_manager  ");
        boolean result = service.canManageBuilding(user, 1);
        assertThat(result).isFalse();
    }

    @Test
    void normalizeRole_handlesNullRole() {
        User user = User.builder().id(1).build();
        boolean result = service.canViewBuilding(user, 1);
        assertThat(result).isFalse();
    }

    @Test
    void getUserBuildingIds_adminRole_returnsEmptyList() {
        java.util.List<Integer> ids = service.getUserBuildingIds(adminUser);
        assertThat(ids).isEmpty();
        verify(permissionRepository, never()).findByUserId(anyInt());
    }

    @Test
    void getUserBuildingIds_nonAdmin_returnsPermissionBuildingIds() {
        UserBuildingPermission perm = UserBuildingPermission.builder()
                .building(building)
                .permissionLevel(BuildingPermissionLevel.VIEW)
                .build();
        when(permissionRepository.findByUserId(anyInt()))
                .thenReturn(java.util.List.of(perm));

        java.util.List<Integer> ids = service.getUserBuildingIds(regularUser);
        assertThat(ids).containsExactly(1);
    }
}
