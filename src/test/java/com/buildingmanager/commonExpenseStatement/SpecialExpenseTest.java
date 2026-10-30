package com.buildingmanager.commonExpenseStatement;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocation;
import com.buildingmanager.commonExpenseAllocation.CommonExpenseAllocationRepository;
import com.buildingmanager.commonExpenseItem.CommonExpenseItem;
import com.buildingmanager.commonExpenseItem.ExpenseCategory;
import com.buildingmanager.email.EmailService;
import com.buildingmanager.notification.NotificationService;
import com.buildingmanager.notificationPreference.NotificationPreferenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SpecialExpenseTest {
    private final CommonExpenseStatementRepository statements = mock(CommonExpenseStatementRepository.class);
    private final ApartmentRepository apartments = mock(ApartmentRepository.class);
    private final CommonExpenseAllocationRepository allocations = mock(CommonExpenseAllocationRepository.class);
    private final BuildingRepository buildings = mock(BuildingRepository.class);
    private final CommonExpenseStatementService service = new CommonExpenseStatementService(
            statements, apartments, allocations, mock(NotificationService.class), buildings, new ObjectMapper(),
            mock(NotificationPreferenceService.class), mock(EmailService.class), mock(StatementPdfService.class));

    private CommonExpenseStatement statement(List<Integer> selected) {
        Building building = Building.builder().id(1).build();
        CommonExpenseItem item = CommonExpenseItem.builder().category(ExpenseCategory.SPECIAL)
                .price(new BigDecimal("10.01")).selectedApartmentIds(selected).build();
        return CommonExpenseStatement.builder().building(building).code("test").month("2026-01")
                .items(new ArrayList<>(List.of(item))).allocations(new ArrayList<>()).build();
    }

    @Test
    void allocatesOnlySelectedApartmentsAndPreservesCents() {
        CommonExpenseStatement statement = statement(List.of(3, 1));
        when(apartments.findAllByBuilding_Id(1)).thenReturn(List.of(
                Apartment.builder().id(3).commonPercent(300.0).build(),
                Apartment.builder().id(2).commonPercent(400.0).build(),
                Apartment.builder().id(1).commonPercent(300.0).build()));
        when(statements.save(any())).thenAnswer(call -> call.getArgument(0));
        when(buildings.findById(1)).thenReturn(Optional.of(statement.getBuilding()));

        service.createAndSend(statement);

        ArgumentCaptor<CommonExpenseAllocation> captor = ArgumentCaptor.forClass(CommonExpenseAllocation.class);
        verify(allocations, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(a -> a.getApartment().getId()).containsExactly(1, 3);
        assertThat(captor.getAllValues()).extracting(CommonExpenseAllocation::getAmount)
                .containsExactly(new BigDecimal("5.01"), new BigDecimal("5.00"));
    }

    @Test
    void rejectsMissingDuplicateAndForeignSelections() {
        when(apartments.findAllByBuilding_Id(1)).thenReturn(List.of(
                Apartment.builder().id(1).commonPercent(1000.0).build()));
        for (List<Integer> ids : List.of(List.<Integer>of(), List.of(1, 1), List.of(2))) {
            assertThatThrownBy(() -> service.createAndSend(statement(ids))).isInstanceOf(IllegalArgumentException.class);
        }
        verify(statements, never()).save(any());
    }

    @Test
    void draftRetainsSelectionAndIssuedUpdateIsRejected() {
        CommonExpenseStatement draft = statement(List.of(1, 2));
        when(statements.save(any())).thenAnswer(call -> call.getArgument(0));
        assertThat(service.saveDraft(draft).getItems().get(0).getSelectedApartmentIds()).containsExactly(1, 2);
        draft.setStatus(StatementStatus.ISSUED);
        when(statements.findById(5)).thenReturn(Optional.of(draft));
        assertThatThrownBy(() -> service.updateStatement(5, new CommonExpenseStatementDTO()))
                .isInstanceOf(IllegalStateException.class);
    }
}
