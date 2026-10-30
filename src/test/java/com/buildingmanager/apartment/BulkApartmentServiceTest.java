package com.buildingmanager.apartment;

import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.user.User;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BulkApartmentServiceTest {
    private final BuildingRepository buildings = mock(BuildingRepository.class);
    private final ApartmentRepository apartments = mock(ApartmentRepository.class);
    private final ApartmentService validation = mock(ApartmentService.class);
    private final BuildingPermissionService permissions = mock(BuildingPermissionService.class);
    private final Authentication auth = mock(Authentication.class);
    private final User user = User.builder().id(1).build();
    private BulkApartmentService service;

    @BeforeEach
    void setup() {
        service = new BulkApartmentService(buildings, apartments, new ApartmentMapper(), validation,
                permissions, Validation.buildDefaultValidatorFactory().getValidator());
        when(auth.getPrincipal()).thenReturn(user);
    }

    private void authorize() {
        when(permissions.canManageBuilding(user, 10)).thenReturn(true);
        when(buildings.findByIdForUpdate(10)).thenReturn(Optional.of(
                Building.builder().id(10).parkingSpacesNum(1).storageNum(1).build()));
    }

    private ApartmentRequest request(String number, String parking, double percentage, Integer ownerId) {
        return new ApartmentRequest(null, " Owner ", " Name ", number, " 1 ", " 50 ", percentage,
                0.0, 0.0, false, "unused", "unused", true, parking, false, null, false,
                null, " description ", false, false, 10, null, ownerId);
    }

    @Test
    void importsNormalizedUnlinkedActiveApartments() {
        authorize();
        when(apartments.save(any())).thenAnswer(invocation -> {
            Apartment apartment = invocation.getArgument(0);
            apartment.setId(42);
            return apartment;
        });
        assertThat(service.importApartments(10, List.of(request(" A ", " P1 ", 100, null)), auth)).containsExactly(42);
        verify(apartments).save(argThat(a -> a.getActive() && a.getEnable() && a.getOwner() == null
                && a.getResident() == null && a.getResidentFirstName() == null
                && a.getNumber().equals("A") && a.getParkingSlot().equals("P1")
                && a.getBuilding().getId().equals(10)));
    }

    @Test
    void rejectsWholeBatchForDuplicatesCapacityAndUnsafeFields() {
        authorize();
        assertThatThrownBy(() -> service.importApartments(10, List.of(
                request(" A ", " P1 ", 10, null), request("a", "p1", Double.NaN, 3)), auth))
                .isInstanceOfSatisfying(BulkApartmentValidationException.class, ex -> {
                    assertThat(ex.getErrors()).extracting(BulkApartmentValidationException.Error::field)
                            .contains("number", "parkingSlot", "parkingSpace", "commonPercent", "ownerId");
                });
        verify(apartments, never()).save(any());
    }

    @Test
    void rejectsUnauthorizedBeforeLoadingBuilding() {
        assertThatThrownBy(() -> service.importApartments(10, List.of(), auth)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(buildings);
    }
}
