package com.company.expense.expense;

public enum ExpenseStatus {
    PENDING_TEAM_LEAD,
    PENDING_FINANCE,
    APPROVED,
    REJECTED;

    public boolean isPending() {
        return this == PENDING_TEAM_LEAD || this == PENDING_FINANCE;
    }
}
