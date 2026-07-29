package com.buildingmanager.user;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.email.EmailService;
import com.buildingmanager.role.Role;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApartmentRepository apartmentRepository;
    @Mock
    private EmailService emailService;

    private Building building;
    private Apartment apt1;
    private Apartment apt2;
    private User owner1;
    private User owner2;
    private User resident1;
    private User testUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, apartmentRepository, emailService);

        building = Building.builder().id(1).name("Test Building").build();

        Role ownerRole = new Role();
        ownerRole.setName("Owner");

        Role residentRole = new Role();
        residentRole.setName("Resident");

        owner1 = User.builder().id(1).firstName("John").lastName("Owner").email("john@test.com").build();
        owner1.setRole(ownerRole);

        owner2 = User.builder().id(2).firstName("Jane").lastName("Owner2").email("jane@test.com").build();
        owner2.setRole(ownerRole);

        resident1 = User.builder().id(3).firstName("Bob").lastName("Resident").email("bob@test.com").build();
        resident1.setRole(residentRole);

        testUser = User.builder().id(10).firstName("Test").lastName("User").email("test@test.com").build();

        apt1 = Apartment.builder()
                .id(1)
                .building(building)
                .number("1")
                .floor("1")
                .owner(owner1)
                .resident(resident1)
                .build();

        apt2 = Apartment.builder()
                .id(2)
                .building(building)
                .number("2")
                .floor("2")
                .owner(owner2)
                .build();
    }

    @Test
    void updateUserRole_updatesRoleWhenUserExists() {
        Role newRole = new Role();
        newRole.setName("BuildingManager");

        User user = User.builder().id(1).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        boolean result = userService.updateUserRole(1, newRole);

        assertThat(result).isTrue();
        assertThat(user.getRole()).isEqualTo(newRole);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserRole_returnsFalseWhenUserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        boolean result = userService.updateUserRole(999, new Role());

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUsersInSameBuilding_returnsUniqueUsersFromBuilding() {
        when(apartmentRepository.findByResident_Id(10)).thenReturn(List.of(apt1));
        when(apartmentRepository.findByOwner_Id(10)).thenReturn(List.of());
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1, apt2));

        List<UserTableDto> users = userService.getUsersInSameBuilding(10);

        assertThat(users).hasSize(3);
        assertThat(users).extracting(UserTableDto::getEmail)
                .containsExactlyInAnyOrder("john@test.com", "bob@test.com", "jane@test.com");
    }

    @Test
    void getUsersInSameBuilding_whenNoApartment_throwsException() {
        when(apartmentRepository.findByResident_Id(10)).thenReturn(List.of());
        when(apartmentRepository.findByOwner_Id(10)).thenReturn(List.of());

        assertThatThrownBy(() -> userService.getUsersInSameBuilding(10))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Δεν βρέθηκε διαμέρισμα");
    }

    @Test
    void findById_returnsUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(owner1));

        Optional<User> result = userService.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(999);

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_returnsUser() {
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(owner1));

        Optional<User> result = userService.findByEmail("john@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("John");
    }

    @Test
    void updateProfessionalsFavoritesOnly_enablesFlag() {
        User user = User.builder().id(1).professionalsFavoritesOnly(false).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        userService.updateProfessionalsFavoritesOnly(1, true);

        assertThat(user.isProfessionalsFavoritesOnly()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void updateProfessionalsFavoritesOnly_disablesFlag() {
        User user = User.builder().id(1).professionalsFavoritesOnly(true).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        userService.updateProfessionalsFavoritesOnly(1, false);

        assertThat(user.isProfessionalsFavoritesOnly()).isFalse();
        verify(userRepository).save(user);
    }
}
