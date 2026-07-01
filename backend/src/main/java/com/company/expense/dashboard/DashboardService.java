package com.company.expense.dashboard;

import com.company.expense.expense.ExpenseRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ExpenseRepository expenseRepository;

    public DashboardService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    /** Total pending expense amounts grouped by department, plus company-wide totals. */
    @Transactional(readOnly = true)
    public DashboardResponse pendingByDepartment() {
        List<PendingByDepartmentRow> rows = expenseRepository.aggregatePendingByDepartment();
        BigDecimal grandTotal = rows.stream()
                .map(PendingByDepartmentRow::totalPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long grandCount = rows.stream()
                .mapToLong(PendingByDepartmentRow::pendingCount)
                .sum();
        return new DashboardResponse(rows, grandTotal, grandCount);
    }
}
