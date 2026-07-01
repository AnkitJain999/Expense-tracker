package com.company.expense.dashboard;

import java.math.BigDecimal;

/**
 * Projection for the pending-by-department aggregation. Populated directly by a
 * JPQL constructor expression in {@code ExpenseRepository}.
 */
public record PendingByDepartmentRow(
        Long departmentId,
        String departmentName,
        BigDecimal totalPendingAmount,
        Long pendingCount,
        BigDecimal pendingTeamLeadAmount,
        BigDecimal pendingFinanceAmount) {
}
