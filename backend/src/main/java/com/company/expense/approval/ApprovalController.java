package com.company.expense.approval;

import com.company.expense.approval.dto.ApprovalDecisionRequest;
import com.company.expense.common.SecurityUtils;
import com.company.expense.expense.dto.ExpenseResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals")
@PreAuthorize("hasAnyRole('TEAM_LEAD', 'FINANCE_MANAGER')")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/pending")
    public List<ExpenseResponse> pending() {
        return approvalService.listPending(SecurityUtils.currentUser());
    }

    @PostMapping("/{expenseId}/approve")
    public ExpenseResponse approve(@PathVariable Long expenseId,
                                   @Valid @RequestBody(required = false) ApprovalDecisionRequest body) {
        String comment = body == null ? null : body.comment();
        return approvalService.approve(SecurityUtils.currentUser(), expenseId, comment);
    }

    @PostMapping("/{expenseId}/reject")
    public ExpenseResponse reject(@PathVariable Long expenseId,
                                  @Valid @RequestBody ApprovalDecisionRequest body) {
        return approvalService.reject(SecurityUtils.currentUser(), expenseId, body.comment());
    }
}
