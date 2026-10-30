package com.buildingmanager.building;

import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.buildingMember.BuildingMember;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.company.Company;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.permission.UserBuildingPermissionRepository;
import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BuildingBulkTest {
    private final BuildingRepository buildings = mock(BuildingRepository.class);
    private final BuildingMemberRepository members = mock(BuildingMemberRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final Role role = Role.builder().name("PropertyManager").build();
    private final User user = User.builder().id(42).role(role).company(mock(Company.class)).build();
    private final Authentication auth = new UsernamePasswordAuthenticationToken(user, null);
    private BuildingService service;

    @BeforeEach
    void setup() {
        service = new BuildingService(buildings, new BuildingMapper(), users, mock(ApartmentRepository.class), members,
                roles, mock(BuildingPermissionService.class), mock(UserBuildingPermissionRepository.class),
                Validation.buildDefaultValidatorFactory().getValidator());
        when(users.findById(42)).thenReturn(Optional.of(user));
        when(roles.findByName("PropertyManager")).thenReturn(Optional.of(role));
        when(buildings.save(any(Building.class))).thenAnswer(invocation -> {
            Building b = invocation.getArgument(0);
            b.setId(123);
            return b;
        });
    }

    private BuildingRequest row(Integer id, String street, String floors) {
        return new BuildingRequest(id, "Building", street, "1", null, null, "City", "Region", "12345", "Country", "State", floors,
                2, 100, 10, false, 0, true, true, null, null, false, null, null, false, false, false, false, false, null);
    }

    @Test
    void createsBuildingAndJoinedMembershipForFreshManager() {
        assertEquals(List.of(123), service.createCompanyBulk(List.of(row(null, "Main", "2")), auth));
        ArgumentCaptor<Building> building = ArgumentCaptor.forClass(Building.class);
        verify(buildings).save(building.capture());
        assertSame(user, building.getValue().getPropertyManager());
        assertSame(user.getCompany(), building.getValue().getCompany());
        assertNull(building.getValue().getManager());
        ArgumentCaptor<BuildingMember> member = ArgumentCaptor.forClass(BuildingMember.class);
        verify(members).save(member.capture());
        assertSame(building.getValue(), member.getValue().getBuilding());
        assertSame(user, member.getValue().getUser());
        assertSame(role, member.getValue().getRole());
        assertEquals(BuildingMemberStatus.JOINED, member.getValue().getStatus());
    }

    @Test
    void rejectsAllRowsBeforeSaving() {
        var ex = assertThrows(BulkBuildingValidationException.class, () -> service.createCompanyBulk(
                List.of(row(null, "Main", "2"), row(7, " main ", "bad"), row(null, "", "1")), auth));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.row() == 2 && e.field().equals("id")));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.row() == 2 && e.field().equals("floors")));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.row() == 2 && e.message().contains("Duplicate")));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.row() == 3 && e.field().equals("street1")));
        verifyNoInteractions(buildings, members);
    }

    @Test
    void checksActualRoleAndCompany() {
        user.setRole(null);
        assertThrows(AccessDeniedException.class, () -> service.createCompanyBulk(List.of(row(null, "Main", "2")), auth));
        user.setRole(role);
        user.setCompany(null);
        assertThrows(AccessDeniedException.class, () -> service.createCompanyBulk(List.of(row(null, "Main", "2")), auth));
        verifyNoInteractions(buildings, members);
    }

    @Test
    void rejectsEmptyAndOversizedBatches() {
        assertThrows(BulkBuildingValidationException.class, () -> service.createCompanyBulk(List.of(), auth));
        assertThrows(BulkBuildingValidationException.class, () -> service.createCompanyBulk(java.util.Collections.nCopies(501, row(null, "Main", "2")), auth));
        verifyNoInteractions(buildings, members);
    }
}
