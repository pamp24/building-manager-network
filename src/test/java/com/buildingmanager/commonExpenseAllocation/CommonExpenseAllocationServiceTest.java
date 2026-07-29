package com.buildingmanager.commonExpenseAllocation;

import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonExpenseAllocationServiceTest {

    private CommonExpenseAllocationService service;

    @Mock
    private CommonExpenseAllocationRepository commonExpenseAllocationRepository;
    @Mock
    private CommonExpenseStatementRepository commonExpenseStatementRepository;

    private CommonExpenseAllocation unpaidAllocation;
    private CommonExpenseAllocation paidAllocation;

    @BeforeEach
    void setUp() {
        service = new CommonExpenseAllocationService(
                commonExpenseAllocationRepository,
                commonExpenseStatementRepository
        );

        unpaidAllocation = CommonExpenseAllocation.builder()
                .id(1)
                .amount(BigDecimal.valueOf(100))
                .paidAmount(BigDecimal.ZERO)
                .isPaid(false)
                .paidDate(null)
                .status("UNPAID")
                .build();

        paidAllocation = CommonExpenseAllocation.builder()
                .id(2)
                .amount(BigDecimal.valueOf(100))
                .paidAmount(BigDecimal.valueOf(100))
                .isPaid(true)
                .paidDate(LocalDateTime.now().minusDays(1))
                .status("PAID")
                .build();
    }

    @Test
    void markAsPaid_setsIsPaidAndPaidDate() {
        when(commonExpenseAllocationRepository.findById(1)).thenReturn(Optional.of(unpaidAllocation));
        when(commonExpenseAllocationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CommonExpenseAllocation result = service.markAsPaid(1);

        assertThat(result.getIsPaid()).isTrue();
        assertThat(result.getPaidDate()).isNotNull();
        assertThat(result.getPaidDate()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void markAsPaid_savesAndReturnsAllocation() {
        when(commonExpenseAllocationRepository.findById(1)).thenReturn(Optional.of(unpaidAllocation));
        when(commonExpenseAllocationRepository.save(any())).thenReturn(unpaidAllocation);

        CommonExpenseAllocation result = service.markAsPaid(1);

        assertThat(result).isNotNull();
        verify(commonExpenseAllocationRepository).save(unpaidAllocation);
    }

    @Test
    void markAsPaid_throwsWhenAllocationNotFound() {
        when(commonExpenseAllocationRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsPaid(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Allocation not found");
    }

    @Test
    void markAsUnpaid_clearsIsPaidAndPaidDate() {
        when(commonExpenseAllocationRepository.findById(2)).thenReturn(Optional.of(paidAllocation));
        when(commonExpenseAllocationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CommonExpenseAllocation result = service.markAsUnpaid(2);

        assertThat(result.getIsPaid()).isFalse();
        assertThat(result.getPaidDate()).isNull();
    }

    @Test
    void markAsUnpaid_throwsWhenAllocationNotFound() {
        when(commonExpenseAllocationRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsUnpaid(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Allocation not found");
    }
}
