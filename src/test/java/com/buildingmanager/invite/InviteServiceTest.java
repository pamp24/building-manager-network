package com.buildingmanager.invite;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.buildingMember.BuildingMember;
import com.buildingmanager.buildingMember.BuildingMemberRepository;
import com.buildingmanager.buildingMember.BuildingMemberStatus;
import com.buildingmanager.company.Company;
import com.buildingmanager.email.EmailService;
import com.buildingmanager.exceptions.BusinessValidationException;
import com.buildingmanager.exceptions.OperationNotPermittedException;
import com.buildingmanager.role.Role;
import com.buildingmanager.role.RoleRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    private InviteService inviteService;

    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private ApartmentRepository apartmentRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BuildingMemberRepository buildingMemberRepository;

    @Captor
    private ArgumentCaptor<Invite> inviteCaptor;
    @Captor
    private ArgumentCaptor<BuildingMember> memberCaptor;

    private User admin;
    private User propertyManager;
    private User regularUser;
    private User inviteRecipient;
    private Apartment apartment;
    private Building building;
    private Company company;
    private Role ownerRole;
    private Role residentRole;
    private Role buildingManagerRole;
    private Role propertyAgentRole;

    @BeforeEach
    void setUp() {
        inviteService = new InviteService(
                inviteRepository, apartmentRepository, emailService,
                roleRepository, userRepository, buildingMemberRepository
        );

        building = Building.builder().id(1).name("Test Building").build();

        company = new Company();
        company.setCompanyName("Test Company");

        admin = User.builder().id(1).email("admin@test.com").build();
        Role adminRole = new Role();
        adminRole.setName("Admin");
        admin.setRole(adminRole);

        propertyManager = User.builder().id(2).email("pm@test.com").company(company).build();
        Role pmRole = new Role();
        pmRole.setName("PropertyManager");
        propertyManager.setRole(pmRole);

        regularUser = User.builder().id(3).email("user@test.com").build();
        Role userRole = new Role();
        userRole.setName("User");
        regularUser.setRole(userRole);

        inviteRecipient = User.builder().id(4).email("recipient@test.com").build();
        inviteRecipient.setRole(userRole);

        apartment = Apartment.builder()
                .id(1)
                .building(building)
                .number("1")
                .floor("1")
                .isRented(true)
                .build();

        ownerRole = new Role();
        ownerRole.setName("Owner");

        residentRole = new Role();
        residentRole.setName("Resident");

        buildingManagerRole = new Role();
        buildingManagerRole.setName("BuildingManager");

        propertyAgentRole = new Role();
        propertyAgentRole.setName("PropertyAgent");
    }

    @Test
    void createInvite_forOwner_savesAndSendsEmail() throws MessagingException {
        when(apartmentRepository.findById(1)).thenReturn(Optional.of(apartment));
        when(inviteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Invite result = inviteService.createInvite("owner@test.com", "Owner", 1, admin);

        assertThat(result.getEmail()).isEqualTo("owner@test.com");
        assertThat(result.getRole()).isEqualTo("Owner");
        assertThat(result.getApartment()).isEqualTo(apartment);
        verify(emailService).sendInviteEmail(
                eq("owner@test.com"),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any()
        );
    }

    @Test
    void createInvite_forOwner_whenApartmentHasOwner_throwsException() {
        apartment.setOwner(User.builder().id(5).build());
        when(apartmentRepository.findById(1)).thenReturn(Optional.of(apartment));

        assertThatThrownBy(() -> inviteService.createInvite("owner@test.com", "Owner", 1, admin))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("ήδη Ιδιοκτήτη");
    }

    @Test
    void createInvite_forOwner_whenPendingInviteExists_throwsException() {
        when(apartmentRepository.findById(1)).thenReturn(Optional.of(apartment));
        when(inviteRepository.existsByApartmentIdAndRoleAndStatus(1, "Owner", InviteStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> inviteService.createInvite("owner@test.com", "Owner", 1, admin))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("ήδη ενεργή πρόσκληση");
    }

    @Test
    void createInvite_forResident_whenApartmentNotRented_throwsException() {
        apartment.setIsRented(false);
        when(apartmentRepository.findById(1)).thenReturn(Optional.of(apartment));

        assertThatThrownBy(() -> inviteService.createInvite("resident@test.com", "Resident", 1, admin))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("δεν είναι προς ενοικίαση");
    }

    @Test
    void createInvite_forResident_whenApartmentHasResident_throwsException() {
        apartment.setResident(User.builder().id(6).build());
        when(apartmentRepository.findById(1)).thenReturn(Optional.of(apartment));

        assertThatThrownBy(() -> inviteService.createInvite("resident@test.com", "Resident", 1, admin))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("ήδη Ένοικο");
    }

    @Test
    void createInvite_forResident_savesAndSendsEmail() throws MessagingException {
        when(apartmentRepository.findById(1)).thenReturn(Optional.of(apartment));
        when(inviteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Invite result = inviteService.createInvite("resident@test.com", "Resident", 1, admin);

        assertThat(result.getRole()).isEqualTo("Resident");
        verify(emailService).sendInviteEmail(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void createInvite_withoutApartmentId_throwsException() {
        assertThatThrownBy(() -> inviteService.createInvite("test@test.com", "Owner", null, admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apartmentId is null");
    }

    @Test
    void createPropertyAgentInvite_byPropertyManager_savesAndSendsEmail() throws MessagingException {
        when(inviteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Invite result = inviteService.createInvite("agent@test.com", "PropertyAgent", null, propertyManager);

        assertThat(result.getRole()).isEqualTo("PropertyAgent");
        assertThat(result.getCompany()).isEqualTo(company);
        verify(emailService).sendInviteEmail(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void createPropertyAgentInvite_byPropertyManagerWithoutCompany_throwsException() {
        propertyManager.setCompany(null);

        assertThatThrownBy(() -> inviteService.createInvite("agent@test.com", "PropertyAgent", null, propertyManager))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("δεν ανήκει σε εταιρία");
    }

    @Test
    void createAdminAgentInvite_byAdmin_savesAndSendsEmail() throws MessagingException {
        when(inviteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Invite result = inviteService.createInvite("adminagent@test.com", "AdminAgent", null, admin);

        assertThat(result.getRole()).isEqualTo("AdminAgent");
        verify(emailService).sendInviteEmail(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void createAdminAgentInvite_byNonAdmin_throwsException() {
        assertThatThrownBy(() -> inviteService.createInvite("adminagent@test.com", "AdminAgent", null, regularUser))
                .isInstanceOf(OperationNotPermittedException.class)
                .hasMessageContaining("Μόνο Admin");
    }

    @Test
    void acceptInvite_forOwner_updatesUserRoleAndApartment() {
        Invite invite = Invite.builder()
                .id(1)
                .email("recipient@test.com")
                .role("Owner")
                .apartment(apartment)
                .inviter(admin)
                .status(InviteStatus.PENDING)
                .token("test-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(inviteRepository.findByToken("test-token")).thenReturn(Optional.of(invite));
        when(userRepository.findByEmail("recipient@test.com")).thenReturn(Optional.of(inviteRecipient));
        when(roleRepository.findByName("Owner")).thenReturn(Optional.of(ownerRole));
        when(inviteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Invite result = inviteService.acceptInvite("test-token", "recipient@test.com");

        assertThat(result.getStatus()).isEqualTo(InviteStatus.ACCEPTED);
        assertThat(inviteRecipient.getRole()).isEqualTo(ownerRole);
        assertThat(apartment.getOwner()).isEqualTo(inviteRecipient);

        verify(buildingMemberRepository).save(memberCaptor.capture());
        BuildingMember member = memberCaptor.getValue();
        assertThat(member.getBuilding()).isEqualTo(building);
        assertThat(member.getUser()).isEqualTo(inviteRecipient);
        assertThat(member.getStatus()).isEqualTo(BuildingMemberStatus.JOINED);
    }

    @Test
    void acceptInvite_forResident_updatesUserRoleAndApartment() {
        Invite invite = Invite.builder()
                .id(2)
                .email("recipient@test.com")
                .role("Resident")
                .apartment(apartment)
                .inviter(admin)
                .status(InviteStatus.PENDING)
                .token("resident-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(inviteRepository.findByToken("resident-token")).thenReturn(Optional.of(invite));
        when(userRepository.findByEmail("recipient@test.com")).thenReturn(Optional.of(inviteRecipient));
        when(roleRepository.findByName("Resident")).thenReturn(Optional.of(residentRole));
        when(inviteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Invite result = inviteService.acceptInvite("resident-token", "recipient@test.com");

        assertThat(result.getStatus()).isEqualTo(InviteStatus.ACCEPTED);
        assertThat(inviteRecipient.getRole()).isEqualTo(residentRole);
        assertThat(apartment.getResident()).isEqualTo(inviteRecipient);
    }

    @Test
    void acceptInvite_forBuildingManager_setsManagerOnBuilding() {
        Invite invite = Invite.builder()
                .id(3)
                .email("recipient@test.com")
                .role("BuildingManager")
                .apartment(apartment)
                .inviter(admin)
                .status(InviteStatus.PENDING)
                .token("bm-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(inviteRepository.findByToken("bm-token")).thenReturn(Optional.of(invite));
        when(userRepository.findByEmail("recipient@test.com")).thenReturn(Optional.of(inviteRecipient));
        when(roleRepository.findByName("BuildingManager")).thenReturn(Optional.of(buildingManagerRole));
        when(inviteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        inviteService.acceptInvite("bm-token", "recipient@test.com");

        assertThat(building.getManager()).isEqualTo(inviteRecipient);
    }

    @Test
    void acceptInvite_forPropertyAgent_setsCompany() {
        Invite invite = Invite.builder()
                .id(4)
                .email("recipient@test.com")
                .role("PropertyAgent")
                .company(company)
                .inviter(propertyManager)
                .status(InviteStatus.PENDING)
                .token("pa-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(inviteRepository.findByToken("pa-token")).thenReturn(Optional.of(invite));
        when(userRepository.findByEmail("recipient@test.com")).thenReturn(Optional.of(inviteRecipient));
        when(roleRepository.findByName("PropertyAgent")).thenReturn(Optional.of(propertyAgentRole));
        when(inviteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        inviteService.acceptInvite("pa-token", "recipient@test.com");

        assertThat(inviteRecipient.getCompany()).isEqualTo(company);
    }

    @Test
    void acceptInvite_expiredToken_throwsException() {
        Invite invite = Invite.builder()
                .id(1)
                .email("recipient@test.com")
                .role("Owner")
                .status(InviteStatus.PENDING)
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(inviteRepository.findByToken("expired-token")).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> inviteService.acceptInvite("expired-token", "recipient@test.com"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Invite expired");

        assertThat(invite.getStatus()).isEqualTo(InviteStatus.EXPIRED);
        verify(inviteRepository).save(invite);
    }

    @Test
    void acceptInvite_emailMismatch_throwsException() {
        Invite invite = Invite.builder()
                .id(1)
                .email("recipient@test.com")
                .role("Owner")
                .status(InviteStatus.PENDING)
                .token("token-123")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(inviteRepository.findByToken("token-123")).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> inviteService.acceptInvite("token-123", "different@test.com"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("email does not match");
    }

    @Test
    void acceptInvite_tokenNotFound_throwsException() {
        when(inviteRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.acceptInvite("invalid-token", "test@test.com"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Invite not found");
    }

    @Test
    void toDTO_convertsInviteCorrectly() {
        Invite invite = Invite.builder()
                .id(1)
                .email("test@test.com")
                .role("Owner")
                .apartment(apartment)
                .token("abc-123")
                .status(InviteStatus.PENDING)
                .build();

        InviteResponseDTO dto = inviteService.toDTO(invite);

        assertThat(dto.getEmail()).isEqualTo("test@test.com");
        assertThat(dto.getRole()).isEqualTo("Owner");
        assertThat(dto.getApartmentId()).isEqualTo(1);
        assertThat(dto.getToken()).isEqualTo("abc-123");
        assertThat(dto.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void toDTO_withNullApartment_returnsNullApartmentId() {
        Invite invite = Invite.builder()
                .id(2)
                .email("agent@test.com")
                .role("PropertyAgent")
                .apartment(null)
                .token("def-456")
                .status(InviteStatus.PENDING)
                .build();

        InviteResponseDTO dto = inviteService.toDTO(invite);

        assertThat(dto.getApartmentId()).isNull();
    }
}
