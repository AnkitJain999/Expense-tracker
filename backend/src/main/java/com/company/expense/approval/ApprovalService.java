package com.company.expense.approval;

import com.company.expense.auth.AppUserPrincipal;
import com.company.expense.common.ApiException;
import com.company.expense.department.Department;
import com.company.expense.department.DepartmentRepository;
import com.company.expense.expense.Expense;
import com.company.expense.expense.ExpenseRepository;
import com.company.expense.expense.ExpenseService;
import com.company.expense.expense.ExpenseStatus;
import com.company.expense.expense.dto.ExpenseResponse;
import com.company.expense.notification.EmailNotificationService;
import com.company.expense.user.Role;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the two-level approval state machine and the routing/authorization rules.
 *
 * <pre>
 * PENDING_TEAM_LEAD --TL approve--> PENDING_FINANCE --FM approve--> APPROVED
 *          |                                 |
 *      TL reject                         FM reject
 *          v                                 v
 *      REJECTED                          REJECTED
 * </pre>
 *
 * A Team Lead rejection sets REJECTED and the expense never enters PENDING_FINANCE.
 */
@Service
public class ApprovalService {

    private final ExpenseRepository expenseRepository;
    private final DepartmentRepository departmentRepository;
    private final ApprovalEventRepository approvalEventRepository;
    private final EmailNotificationService notifications;
    private final ExpenseService expenseService;

    public ApprovalService(ExpenseRepository expenseRepository,
                           DepartmentRepository departmentRepository,
                           ApprovalEventRepository approvalEventRepository,
                           EmailNotificationService notifications,
                           ExpenseService expenseService) {
        this.expenseRepository = expenseRepository;
        this.departmentRepository = departmentRepository;
        this.approvalEventRepository = approvalEventRepository;
        this.notifications = notifications;
        this.expenseService = expenseService;
    }

    /** Pending items awaiting the current approver at their stage, in their department(s). */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> listPending(AppUserPrincipal principal) {
        List<Long> departmentIds = departmentsFor(principal).stream().map(Department::getId).toList();
        if (departmentIds.isEmpty()) {
            return List.of();
        }
        ExpenseStatus stage = stageFor(principal.getRole());
        List<Expense> pending = expenseRepository
                .findByStatusAndDepartmentIdInOrderByCreatedAtAsc(stage, departmentIds);
        return expenseService.toResponses(pending);
    }

    @Transactional
    public ExpenseResponse approve(AppUserPrincipal principal, Long expenseId, String comment) {
        Expense expense = loadForDecision(principal, expenseId);
        Role role = principal.getRole();

        if (role == Role.TEAM_LEAD) {
            expense.setStatus(ExpenseStatus.PENDING_FINANCE);
            expenseRepository.save(expense);
            record(expense, principal, ApprovalAction.TEAM_LEAD_APPROVED, comment);
            notifications.notifyTeamLeadApproved(expense);
        } else { // FINANCE_MANAGER
            expense.setStatus(ExpenseStatus.APPROVED);
            expenseRepository.save(expense);
            record(expense, principal, ApprovalAction.FINANCE_APPROVED, comment);
            notifications.notifyDecision(expense, true, "Finance Manager", comment);
        }
        return expenseService.toResponses(List.of(expense)).get(0);
    }

    @Transactional
    public ExpenseResponse reject(AppUserPrincipal principal, Long expenseId, String comment) {
        if (comment == null || comment.isBlank()) {
            throw ApiException.badRequest("A comment is required when rejecting an expense");
        }
        Expense expense = loadForDecision(principal, expenseId);
        Role role = principal.getRole();

        // Rejection at either level halts progression: status becomes REJECTED and the
        // expense is never advanced to the next stage.
        expense.setStatus(ExpenseStatus.REJECTED);
        expenseRepository.save(expense);

        if (role == Role.TEAM_LEAD) {
            record(expense, principal, ApprovalAction.TEAM_LEAD_REJECTED, comment);
            notifications.notifyDecision(expense, false, "Team Lead", comment);
        } else {
            record(expense, principal, ApprovalAction.FINANCE_REJECTED, comment);
            notifications.notifyDecision(expense, false, "Finance Manager", comment);
        }
        return expenseService.toResponses(List.of(expense)).get(0);
    }

    /**
     * Loads the expense and enforces that (a) the caller holds an approver role,
     * (b) the expense is at the caller's stage, and (c) the caller is the assigned
     * approver for the expense's department.
     */
    private Expense loadForDecision(AppUserPrincipal principal, Long expenseId) {
        Role role = principal.getRole();
        if (role != Role.TEAM_LEAD && role != Role.FINANCE_MANAGER) {
            throw ApiException.forbidden("Only approvers may act on expenses");
        }
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> ApiException.notFound("Expense not found"));

        ExpenseStatus expectedStage = stageFor(role);
        if (expense.getStatus() != expectedStage) {
            throw ApiException.conflict("Expense is not awaiting your approval (status: "
                    + expense.getStatus() + ")");
        }

        Department dept = departmentRepository.findById(expense.getDepartmentId())
                .orElseThrow(() -> ApiException.notFound("Department not found"));
        boolean assigned = role == Role.TEAM_LEAD
                ? principal.getId().equals(dept.getTeamLeadId())
                : principal.getId().equals(dept.getFinanceManagerId());
        if (!assigned) {
            throw ApiException.forbidden("You are not the assigned approver for this department");
        }
        return expense;
    }

    private void record(Expense expense, AppUserPrincipal principal, ApprovalAction action, String comment) {
        approvalEventRepository.save(new ApprovalEvent(expense.getId(), principal.getId(), action, comment));
    }

    private List<Department> departmentsFor(AppUserPrincipal principal) {
        return switch (principal.getRole()) {
            case TEAM_LEAD -> departmentRepository.findByTeamLeadId(principal.getId());
            case FINANCE_MANAGER -> departmentRepository.findByFinanceManagerId(principal.getId());
            default -> List.of();
        };
    }

    private static ExpenseStatus stageFor(Role role) {
        return role == Role.TEAM_LEAD ? ExpenseStatus.PENDING_TEAM_LEAD : ExpenseStatus.PENDING_FINANCE;
    }
}
