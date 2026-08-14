package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import com.buildingmanager.commonExpenseItem.CommonExpenseItem;
import com.buildingmanager.commonExpenseItem.ExpenseCategory;
import com.buildingmanager.notification.NotificationService;
import com.buildingmanager.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonExpenseStatementServiceTest {

    private CommonExpenseStatementService service;

    @Mock
    private CommonExpenseStatementRepository commonExpenseStatementRepository;
    @Mock
    private ApartmentRepository apartmentRepository;
    @Mock
    private CommonExpenseAllocationRepository commonExpenseAllocationRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BuildingRepository buildingRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Captor
    private ArgumentCaptor<CommonExpenseStatement> statementCaptor;
    @Captor
    private ArgumentCaptor<CommonExpenseAllocation> allocationCaptor;

    private Building building;
    private Apartment apt1;
    private Apartment apt2;
    private User owner1;
    private User resident2;
    private User buildingManager;

    @BeforeEach
    void setUp() {
        service = new CommonExpenseStatementService(
                commonExpenseStatementRepository, apartmentRepository,
                commonExpenseAllocationRepository, notificationService,
                buildingRepository, objectMapper
        );

        building = Building.builder().id(1).name("Test Building").build();

        owner1 = User.builder().id(1).firstName("John").lastName("Doe").email("john@test.com").build();
        resident2 = User.builder().id(2).firstName("Jane").lastName("Smith").email("jane@test.com").build();
        buildingManager = User.builder().id(3).firstName("Bob").lastName("Manager").email("bob@test.com").build();

        apt1 = Apartment.builder()
                .id(1)
                .building(building)
                .number("1")
                .floor("1")
                .commonPercent(500.0)
                .elevatorPercent(500.0)
                .heatingPercent(500.0)
                .owner(owner1)
                .build();

        apt2 = Apartment.builder()
                .id(2)
                .building(building)
                .number("2")
                .floor("2")
                .commonPercent(500.0)
                .elevatorPercent(500.0)
                .heatingPercent(500.0)
                .resident(resident2)
                .owner(owner1)
                .build();
    }

    private CommonExpenseStatement createStatementWithItems(List<CommonExpenseItem> items) {
        CommonExpenseStatement statement = CommonExpenseStatement.builder()
                .building(building)
                .code("TEST-001")
                .month("2026-07")
                .type("ΜΗΝΙΑΙΟ")
                .discountPercent(BigDecimal.ZERO)
                .taxPercent(BigDecimal.valueOf(24))
                .items(items)
                .status(StatementStatus.ISSUED)
                .build();

        items.forEach(i -> i.setStatement(statement));
        return statement;
    }

    @Test
    void createAndSend_calculatesSubTotalCorrectly() {
        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.COMMON).price(BigDecimal.valueOf(100)).descriptionItem("Cleaning").build(),
                CommonExpenseItem.builder().category(ExpenseCategory.HEATING).price(BigDecimal.valueOf(200)).descriptionItem("Fuel").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(0);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1, apt2));
        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));

        CommonExpenseStatement result = service.createAndSend(statement);

        assertThat(result.getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(300));
        verify(commonExpenseStatementRepository).save(statementCaptor.capture());
        assertThat(statementCaptor.getValue().getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    void createAndSend_calculatesTotalWithTaxCorrectly() {
        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.COMMON).price(BigDecimal.valueOf(100)).descriptionItem("Cleaning").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);
        statement.setTaxPercent(BigDecimal.valueOf(24));

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(0);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1, apt2));
        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));

        CommonExpenseStatement result = service.createAndSend(statement);

        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(124));
    }

    @Test
    void createAndSend_calculatesTotalWithDiscountCorrectly() {
        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.COMMON).price(BigDecimal.valueOf(100)).descriptionItem("Cleaning").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);
        statement.setDiscountPercent(BigDecimal.valueOf(10));
        statement.setTaxPercent(BigDecimal.ZERO);

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(0);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1, apt2));
        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));

        CommonExpenseStatement result = service.createAndSend(statement);

        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(90));
    }

    @Test
    void createAndSend_allocatesCommonExpenseByCommonPercent() {
        apt1.setCommonPercent(600.0);
        apt2.setCommonPercent(400.0);

        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.COMMON).price(BigDecimal.valueOf(100)).descriptionItem("Cleaning").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(0);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1, apt2));
        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));

        service.createAndSend(statement);

        verify(commonExpenseAllocationRepository, times(2)).save(allocationCaptor.capture());
        List<CommonExpenseAllocation> allocations = allocationCaptor.getAllValues();

        CommonExpenseAllocation allocApt1 = allocations.stream()
                .filter(a -> a.getApartment().getId().equals(1))
                .findFirst().orElseThrow();
        CommonExpenseAllocation allocApt2 = allocations.stream()
                .filter(a -> a.getApartment().getId().equals(2))
                .findFirst().orElseThrow();

        assertThat(allocApt1.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(60.00));
        assertThat(allocApt2.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(40.00));
    }

    @Test
    void createAndSend_roundsProportionalSharesAndDistributesRemainderToLargestFraction() {
        apt1.setCommonPercent(333.0);
        apt2.setCommonPercent(333.0);
        Apartment apt3 = Apartment.builder()
                .id(3)
                .building(building)
                .number("3")
                .floor("3")
                .commonPercent(334.0)
                .elevatorPercent(334.0)
                .heatingPercent(334.0)
                .owner(owner1)
                .build();

        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.COMMON).price(BigDecimal.valueOf(10.01)).descriptionItem("Common").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(0);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1, apt2, apt3));
        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));

        service.createAndSend(statement);

        verify(commonExpenseAllocationRepository, times(3)).save(allocationCaptor.capture());
        List<CommonExpenseAllocation> allocations = allocationCaptor.getAllValues();

        BigDecimal total = allocations.stream()
                .map(CommonExpenseAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Το άθροισμα πρέπει να ισούται ακριβώς με το itemTotal
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(10.01));

        // Το +0.01 πάει στο διαμέρισμα με το μεγαλύτερο κλασματικό υπόλοιπο (apt3 με 334 χιλιοστά)
        CommonExpenseAllocation allocApt3 = allocations.stream()
                .filter(a -> a.getApartment().getId().equals(3))
                .findFirst().orElseThrow();
        assertThat(allocApt3.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3.35));
        assertThat(allocations.stream()
                .filter(a -> a.getApartment().getId().equals(1))
                .findFirst().orElseThrow().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3.33));
        assertThat(allocations.stream()
                .filter(a -> a.getApartment().getId().equals(2))
                .findFirst().orElseThrow().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3.33));
    }

    @Test
    void createAndSend_allocatesEqualExpenseEqually() {
        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.EQUAL).price(BigDecimal.valueOf(101)).descriptionItem("Equal share").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(0);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1, apt2));
        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));

        service.createAndSend(statement);

        verify(commonExpenseAllocationRepository, times(2)).save(allocationCaptor.capture());
        List<CommonExpenseAllocation> allocations = allocationCaptor.getAllValues();

        BigDecimal total = allocations.stream()
                .map(CommonExpenseAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(101));
        BigDecimal half = BigDecimal.valueOf(101).divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        assertThat(allocations.get(0).getAmount()).isIn(half, half.add(BigDecimal.valueOf(0.01)));
    }

    @Test
    void createAndSend_heatingAllocationUsesHeatingPercent() {
        apt1.setHeatingPercent(700.0);
        apt2.setHeatingPercent(300.0);

        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.HEATING).price(BigDecimal.valueOf(200)).descriptionItem("Heating oil").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(0);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1, apt2));
        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));

        service.createAndSend(statement);

        verify(commonExpenseAllocationRepository, times(2)).save(allocationCaptor.capture());
        CommonExpenseAllocation allocApt2 = allocationCaptor.getAllValues().stream()
                .filter(a -> a.getApartment().getId().equals(2))
                .findFirst().orElseThrow();

        assertThat(allocApt2.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(60.00));
    }

    @Test
    void createAndSend_assignsOwnerForOwnersCategory() {
        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.OWNERS).price(BigDecimal.valueOf(50)).descriptionItem("Owner expense").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(0);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1, apt2));
        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));

        service.createAndSend(statement);

        verify(commonExpenseAllocationRepository, times(2)).save(allocationCaptor.capture());
        List<CommonExpenseAllocation> allocations = allocationCaptor.getAllValues();

        allocations.forEach(a -> assertThat(a.getUser()).isEqualTo(owner1));
    }

    @Test
    void createAndSend_assignsResidentForNonOwnersCategory() {
        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.COMMON).price(BigDecimal.valueOf(100)).descriptionItem("Common").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(0);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1, apt2));
        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));

        service.createAndSend(statement);

        verify(commonExpenseAllocationRepository, times(2)).save(allocationCaptor.capture());
        List<CommonExpenseAllocation> allocations = allocationCaptor.getAllValues();

        CommonExpenseAllocation allocApt1 = allocations.stream().filter(a -> a.getApartment().getId().equals(1)).findFirst().orElseThrow();
        CommonExpenseAllocation allocApt2 = allocations.stream().filter(a -> a.getApartment().getId().equals(2)).findFirst().orElseThrow();

        assertThat(allocApt1.getUser()).isEqualTo(owner1);
        assertThat(allocApt2.getUser()).isEqualTo(resident2);
    }

    @Test
    void createAndSend_incrementsSequenceNumber() {
        apt1.setCommonPercent(1000.0);

        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.COMMON).price(BigDecimal.valueOf(100)).descriptionItem("Test").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(5);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1));
        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));

        CommonExpenseStatement result = service.createAndSend(statement);

        assertThat(result.getSequenceNumber()).isEqualTo(6);
    }

    @Test
    void createAndSend_sendsNotificationToOwnerResidentAndManager() {
        building.setManager(buildingManager);

        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.COMMON).price(BigDecimal.valueOf(100)).descriptionItem("Test").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(0);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(apartmentRepository.findAllByBuilding_Id(1)).thenReturn(List.of(apt1, apt2));
        when(buildingRepository.findById(1)).thenReturn(Optional.of(building));

        service.createAndSend(statement);

        verify(notificationService, times(3)).create(any(), eq("NEW_STATEMENT"), anyString(), any());
        verify(notificationService).create(eq(owner1), eq("NEW_STATEMENT"), anyString(), any());
        verify(notificationService).create(eq(resident2), eq("NEW_STATEMENT"), anyString(), any());
        verify(notificationService).create(eq(buildingManager), eq("NEW_STATEMENT"), anyString(), any());
    }

    @Test
    void saveDraft_createsStatementWithDraftStatus() {
        List<CommonExpenseItem> items = List.of(
                CommonExpenseItem.builder().category(ExpenseCategory.COMMON).price(BigDecimal.valueOf(100)).descriptionItem("Test").build()
        );
        CommonExpenseStatement statement = createStatementWithItems(items);

        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(null);
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CommonExpenseStatement result = service.saveDraft(statement);

        assertThat(result.getStatus()).isEqualTo(StatementStatus.DRAFT);
        assertThat(result.getSequenceNumber()).isEqualTo(1);
        assertThat(result.getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void delete_withPayments_throwsException() {
        when(commonExpenseAllocationRepository.hasAnyPaymentForStatement(1)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Δεν επιτρέπεται");
    }

    @Test
    void updateStatement_withPayments_throwsException() {
        when(commonExpenseAllocationRepository.hasAnyPaymentForStatement(1)).thenReturn(true);

        CommonExpenseStatementDTO dto = new CommonExpenseStatementDTO();

        assertThatThrownBy(() -> service.updateStatement(1, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Δεν επιτρέπεται");
    }

    @Test
    void updateStatement_recalculatesTotals() {
        CommonExpenseStatement statement = CommonExpenseStatement.builder()
                .id(1)
                .building(building)
                .items(new ArrayList<>(List.of(
                        CommonExpenseItem.builder().category(ExpenseCategory.COMMON).price(BigDecimal.valueOf(50)).descriptionItem("Old").build()
                )))
                .allocations(new ArrayList<>())
                .build();

        when(commonExpenseAllocationRepository.hasAnyPaymentForStatement(1)).thenReturn(false);
        when(commonExpenseStatementRepository.findById(1)).thenReturn(Optional.of(statement));
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CommonExpenseStatementDTO dto = CommonExpenseStatementDTO.builder()
                .code("UPD-001")
                .month("2026-08")
                .discountPercent(BigDecimal.ZERO)
                .taxPercent(BigDecimal.valueOf(24))
                .items(List.of(
                        com.buildingmanager.commonExpenseItem.CommonExpenseItemDTO.builder()
                                .category("COMMON")
                                .price(BigDecimal.valueOf(200))
                                .descriptionItem("Updated")
                                .build()
                ))
                .build();

        CommonExpenseStatementDTO result = service.updateStatement(1, dto);

        assertThat(result.getSubTotal()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(248));
    }

    @Test
    void generateNextCode_generatesCorrectFormat() {
        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(42);

        String code = service.generateNextCode(1);

        assertThat(code).matches("\\d{8}-000001-000043");
    }

    @Test
    void generateNextCode_withNoPreviousSequence_startsAt1() {
        when(commonExpenseStatementRepository.findMaxSequenceByBuilding(1)).thenReturn(null);

        String code = service.generateNextCode(1);

        assertThat(code).endsWith("-000001-000001");
    }

    @Test
    void getStatementsByBuilding_updatesExpiredStatements() {
        CommonExpenseStatement expiredStatement = CommonExpenseStatement.builder()
                .id(1)
                .building(building)
                .code("EXP-001")
                .status(StatementStatus.ISSUED)
                .isPaid(false)
                .endDate(LocalDateTime.now().minusDays(1))
                .items(List.of())
                .allocations(List.of())
                .build();

        when(commonExpenseStatementRepository.findByBuildingIdOrderByStartDateDesc(1)).thenReturn(List.of(expiredStatement));
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(commonExpenseAllocationRepository.hasAnyPaymentForStatement(1)).thenReturn(false);

        List<CommonExpenseStatementDTO> result = service.getStatementsByBuilding(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(StatementStatus.EXPIRED);
    }

    @Test
    void getStatementsByBuilding_updatesPaidStatus_whenIsPaidTrue() {
        CommonExpenseStatement paidStatement = CommonExpenseStatement.builder()
                .id(2)
                .building(building)
                .code("PAID-001")
                .status(StatementStatus.ISSUED)
                .isPaid(true)
                .endDate(LocalDateTime.now().plusDays(1))
                .items(List.of())
                .allocations(List.of())
                .build();

        when(commonExpenseStatementRepository.findByBuildingIdOrderByStartDateDesc(1)).thenReturn(List.of(paidStatement));
        when(commonExpenseStatementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(commonExpenseAllocationRepository.hasAnyPaymentForStatement(2)).thenReturn(true);

        List<CommonExpenseStatementDTO> result = service.getStatementsByBuilding(1);

        assertThat(result.get(0).getStatus()).isEqualTo(StatementStatus.PAID);
        assertThat(result.get(0).isHasPayments()).isTrue();
    }

    @Test
    void getActiveStatementsByBuildingDTO_returnsOnlyActiveStatements() {
        CommonExpenseStatement activeStatement = CommonExpenseStatement.builder()
                .id(3)
                .building(building)
                .code("ACT-001")
                .status(StatementStatus.ISSUED)
                .items(List.of())
                .allocations(List.of())
                .build();

        when(commonExpenseStatementRepository.findActiveByBuildingId(1)).thenReturn(List.of(activeStatement));
        when(commonExpenseAllocationRepository.hasAnyPaymentForStatement(3)).thenReturn(false);

        List<CommonExpenseStatementDTO> result = service.getActiveStatementsByBuildingDTO(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("ACT-001");
    }

    @Test
    void getAll_returnsAllStatements() {
        when(commonExpenseStatementRepository.findAll()).thenReturn(List.of(
                CommonExpenseStatement.builder().id(1).build(),
                CommonExpenseStatement.builder().id(2).build()
        ));

        List<CommonExpenseStatement> result = service.getAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(commonExpenseStatementRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Δεν βρέθηκε");
    }

    @Test
    void getById_returnsStatementWhenFound() {
        CommonExpenseStatement statement = CommonExpenseStatement.builder().id(1).code("FOUND").build();
        when(commonExpenseStatementRepository.findById(1)).thenReturn(Optional.of(statement));

        CommonExpenseStatement result = service.getById(1);

        assertThat(result.getCode()).isEqualTo("FOUND");
    }
}
