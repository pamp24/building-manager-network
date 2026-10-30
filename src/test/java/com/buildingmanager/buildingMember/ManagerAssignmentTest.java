package com.buildingmanager.buildingMember;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.company.Company;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.role.Role;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerAssignmentTest {
    @Mock BuildingMemberRepository members;
    @Mock BuildingRepository buildings;
    @Mock UserRepository users;
    @Mock Authentication auth;
    @Mock BuildingPermissionService permissions;
    BuildingMemberService service;
    Building building;
    User actor;
    User target;
    BuildingMember member;

    @BeforeEach
    void setup() {
        service = new BuildingMemberService(null, members, buildings, users, null, null, null, null, permissions);
        Company company = new Company();
        company.setId(7);
        Role role = new Role();
        role.setName("PropertyManager");
        actor = User.builder().id(1).role(role).company(company).build();
        target = User.builder().id(2).build();
        building = new Building();
        building.setId(3);
        building.setCompany(company);
        member = BuildingMember.builder().building(building).user(target).status(BuildingMemberStatus.JOINED).build();
        when(auth.getName()).thenReturn("pm@example.com");
        when(users.findByEmail("pm@example.com")).thenReturn(Optional.of(actor));
        when(buildings.findByIdForUpdate(3)).thenReturn(Optional.of(building));
    }

    @Test
    void replacesOnlyDesignation() {
        User old = User.builder().id(4).build();
        building.setManager(old);
        when(members.findById(5)).thenReturn(Optional.of(member));
        service.assignBuildingManager(3, 5, auth);
        assertThat(building.getManager()).isSameAs(target);
        assertThat(member.getStatus()).isEqualTo(BuildingMemberStatus.JOINED);
        verify(users, never()).save(any());
        verify(members, never()).save(any());
    }

    @Test
    void rejectsRemovedAndCrossBuildingMembers() {
        when(members.findById(5)).thenReturn(Optional.of(member));
        member.setStatus(BuildingMemberStatus.REMOVED);
        assertThatThrownBy(() -> service.assignBuildingManager(3, 5, auth)).isInstanceOf(IllegalArgumentException.class);
        member.setStatus(BuildingMemberStatus.JOINED);
        Building other = new Building();
        other.setId(9);
        member.setBuilding(other);
        assertThatThrownBy(() -> service.assignBuildingManager(3, 5, auth)).isInstanceOf(IllegalArgumentException.class);
        verify(buildings, never()).save(any());
    }

    @Test
    void rejectsUnrelatedActor() {
        actor.setCompany(null);
        assertThatThrownBy(() -> service.assignBuildingManager(3, 5, auth)).isInstanceOf(AccessDeniedException.class);
        verify(members, never()).findById(any());
    }
}
