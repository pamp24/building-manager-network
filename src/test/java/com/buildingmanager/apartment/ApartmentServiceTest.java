package com.buildingmanager.apartment;

import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.invite.InviteRepository;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApartmentServiceTest {

    private ApartmentService service;

    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private ApartmentMapper apartmentMapper;
    @Mock
    private ApartmentRepository apartmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private BuildingPermissionService buildingPermissionService;
    @Mock
    private Authentication authentication;

    private User manager;

    @BeforeEach
    void setUp() {
        service = new ApartmentService(
                buildingRepository, apartmentMapper, apartmentRepository,
                userRepository, inviteRepository, buildingPermissionService
        );

        manager = User.builder().id(1).firstName("Bob").lastName("Manager").build();
        when(authentication.getPrincipal()).thenReturn(manager);
    }

    @Test
    void redistributeCommonPercent_distributesRemainderEqually() {
        when(buildingPermissionService.canManageBuilding(manager, 10)).thenReturn(true);

        Apartment apt1 = apartment(1, 100.0);
        Apartment apt2 = apartment(2, 200.0);
        Apartment apt3 = apartment(3, 300.0);

        when(apartmentRepository.findAllByBuilding_IdAndActiveTrueAndEnableTrueOrderByFloorAscNumberAsc(10))
                .thenReturn(List.of(apt1, apt2, apt3));

        service.redistributeCommonPercent(10, authentication);

        double sum = apt1.getCommonPercent() + apt2.getCommonPercent() + apt3.getCommonPercent();
        assertThat(sum).isEqualTo(1000.0);
        assertThat(apt1.getCommonPercent()).isEqualTo(233.34);
        assertThat(apt2.getCommonPercent()).isEqualTo(333.33);
        assertThat(apt3.getCommonPercent()).isEqualTo(433.33);
        verify(apartmentRepository, times(3)).save(any(Apartment.class));
    }

    @Test
    void redistributeCommonPercent_whenAlready1000_doesNotChange() {
        when(buildingPermissionService.canManageBuilding(manager, 10)).thenReturn(true);

        Apartment apt1 = apartment(1, 600.0);
        Apartment apt2 = apartment(2, 400.0);

        when(apartmentRepository.findAllByBuilding_IdAndActiveTrueAndEnableTrueOrderByFloorAscNumberAsc(10))
                .thenReturn(List.of(apt1, apt2));

        service.redistributeCommonPercent(10, authentication);

        assertThat(apt1.getCommonPercent()).isEqualTo(600.0);
        assertThat(apt2.getCommonPercent()).isEqualTo(400.0);
        verify(apartmentRepository, never()).save(any(Apartment.class));
    }

    private Apartment apartment(int id, Double commonPercent) {
        return Apartment.builder()
                .id(id)
                .commonPercent(commonPercent)
                .build();
    }
}
