package com.company.expense.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.expense.auth.AppUserPrincipal;
import com.company.expense.common.ApiException;
import com.company.expense.department.Department;
import com.company.expense.department.DepartmentRepository;
import com.company.expense.expense.Category;
import com.company.expense.expense.Expense;
import com.company.expense.expense.ExpenseRepository;
import com.company.expense.expense.ExpenseService;
import com.company.expense.expense.ExpenseStatus;
import com.company.expense.expense.dto.ExpenseResponse;
import com.company.expense.notification.EmailNotificationService;
import com.company.expense.user.Role;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    private static final long ENG_DEPT = 1L;
    private static final long TEAM_LEAD_ID = 10L;
    private static final long FINANCE_ID = 20L;
    private static final long EMPLOYEE_ID = 30L;

    @Mock
    ExpenseRepository expenseRepository;
    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    ApprovalEventRepository approvalEventRepository;
    @Mock
    EmailNotificationService notifications;
    @Mock
    ExpenseService expenseService;

    @InjectMocks
    ApprovalService approvalService;

    Department engineering;

    @BeforeEach
    void setup() {
        engineering = new Department("Engineering", TEAM_LEAD_ID, FINANCE_ID);
        // toResponses is only used to build the return value; a stub is fine.
        lenientToResponses();
    }

    private void lenientToResponses() {
        lenient().when(expenseService.toResponses(any())).thenReturn(List.of(
                new ExpenseResponse(1L, Category.TRAVEL, BigDecimal.TEN, "USD", "d",
                        ExpenseStatus.APPROVED, EMPLOYEE_ID, "Emp", ENG_DEPT, "Engineering",
                        false, null, null)));
    }

    private AppUserPrincipal teamLead() {
        return new AppUserPrincipal(TEAM_LEAD_ID, "tl@co", "x", Role.TEAM_LEAD, ENG_DEPT, true);
    }

    private AppUserPrincipal financeManager() {
        return new AppUserPrincipal(FINANCE_ID, "fm@co", "x", Role.FINANCE_MANAGER, null, true);
    }

    private Expense expense(ExpenseStatus status) {
        Expense e = new Expense(EMPLOYEE_ID, ENG_DEPT, Category.TRAVEL, new BigDecimal("100.00"),
                "USD", "Taxi");
        e.setStatus(status);
        setId(e, 1L);
        return e;
    }

    @Test
    void teamLeadApprove_advancesToPendingFinance_andRecordsEvent() {
        Expense e = expense(ExpenseStatus.PENDING_TEAM_LEAD);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));
        when(departmentRepository.findById(ENG_DEPT)).thenReturn(Optional.of(engineering));

        approvalService.approve(teamLead(), 1L, "ok");

        assertThat(e.getStatus()).isEqualTo(ExpenseStatus.PENDING_FINANCE);
        ArgumentCaptor<ApprovalEvent> captor = ArgumentCaptor.forClass(ApprovalEvent.class);
        verify(approvalEventRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(ApprovalAction.TEAM_LEAD_APPROVED);
        verify(notifications).notifyTeamLeadApproved(e);
    }

    @Test
    void teamLeadReject_setsRejected_andNeverReachesFinance() {
        Expense e = expense(ExpenseStatus.PENDING_TEAM_LEAD);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));
        when(departmentRepository.findById(ENG_DEPT)).thenReturn(Optional.of(engineering));

        approvalService.reject(teamLead(), 1L, "over budget");

        assertThat(e.getStatus()).isEqualTo(ExpenseStatus.REJECTED);
        ArgumentCaptor<ApprovalEvent> captor = ArgumentCaptor.forClass(ApprovalEvent.class);
        verify(approvalEventRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(ApprovalAction.TEAM_LEAD_REJECTED);
        // Halted: finance manager is never notified about advancement.
        verify(notifications, never()).notifyTeamLeadApproved(any());
        verify(notifications).notifyDecision(eq(e), eq(false), anyString(), anyString());
    }

    @Test
    void financeApprove_setsApproved() {
        Expense e = expense(ExpenseStatus.PENDING_FINANCE);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));
        when(departmentRepository.findById(ENG_DEPT)).thenReturn(Optional.of(engineering));

        approvalService.approve(financeManager(), 1L, null);

        assertThat(e.getStatus()).isEqualTo(ExpenseStatus.APPROVED);
        verify(notifications).notifyDecision(eq(e), eq(true), anyString(), any());
    }

    @Test
    void reject_withoutComment_isRejected() {
        assertThatThrownBy(() -> approvalService.reject(teamLead(), 1L, "  "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("comment is required");
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void teamLead_cannotActOnExpenseAwaitingFinance() {
        Expense e = expense(ExpenseStatus.PENDING_FINANCE);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> approvalService.approve(teamLead(), 1L, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not awaiting your approval");
    }

    @Test
    void approver_fromDifferentDepartment_isForbidden() {
        Expense e = expense(ExpenseStatus.PENDING_TEAM_LEAD);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));
        when(departmentRepository.findById(ENG_DEPT)).thenReturn(Optional.of(engineering));
        // A different team lead (id 99) than the department's assigned lead (10).
        AppUserPrincipal otherLead = new AppUserPrincipal(99L, "o@co", "x", Role.TEAM_LEAD, 2L, true);

        assertThatThrownBy(() -> approvalService.approve(otherLead, 1L, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("assigned approver");
    }

    @Test
    void employee_cannotApprove() {
        AppUserPrincipal employee = new AppUserPrincipal(EMPLOYEE_ID, "e@co", "x", Role.EMPLOYEE, ENG_DEPT, true);
        assertThatThrownBy(() -> approvalService.approve(employee, 1L, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only approvers");
    }

    private static void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
