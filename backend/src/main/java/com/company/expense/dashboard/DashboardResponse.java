package com.company.expense.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        List<PendingByDepartmentRow> departments,
        BigDecimal grandTotalPending,
        long grandTotalCount) {
}
