package com.company.expense.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.expense.expense.ExpenseRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    ExpenseRepository expenseRepository;

    @InjectMocks
    DashboardService dashboardService;

    @Test
    void pendingByDepartment_sumsGrandTotalsAcrossDepartments() {
        when(expenseRepository.aggregatePendingByDepartment()).thenReturn(List.of(
                new PendingByDepartmentRow(1L, "Engineering", new BigDecimal("605.50"), 2L,
                        new BigDecimal("125.50"), new BigDecimal("480.00")),
                new PendingByDepartmentRow(2L, "Marketing", new BigDecimal("460.00"), 2L,
                        new BigDecimal("310.00"), new BigDecimal("150.00"))));

        DashboardResponse response = dashboardService.pendingByDepartment();

        assertThat(response.departments()).hasSize(2);
        assertThat(response.grandTotalPending()).isEqualByComparingTo("1065.50");
        assertThat(response.grandTotalCount()).isEqualTo(4L);
    }

    @Test
    void pendingByDepartment_emptyIsZero() {
        when(expenseRepository.aggregatePendingByDepartment()).thenReturn(List.of());

        DashboardResponse response = dashboardService.pendingByDepartment();

        assertThat(response.grandTotalPending()).isEqualByComparingTo("0");
        assertThat(response.grandTotalCount()).isZero();
    }
}
