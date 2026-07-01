package com.company.expense.expense.dto;

import com.company.expense.expense.Category;
import com.company.expense.expense.Expense;
import com.company.expense.expense.ExpenseStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseResponse(
        Long id,
        Category category,
        BigDecimal amount,
        String currency,
        String description,
        ExpenseStatus status,
        Long submitterId,
        String submitterName,
        Long departmentId,
        String departmentName,
        boolean hasReceipt,
        Instant createdAt,
        Instant updatedAt) {

    public static ExpenseResponse of(Expense e, String submitterName, String departmentName,
                                     boolean hasReceipt) {
        return new ExpenseResponse(e.getId(), e.getCategory(), e.getAmount(), e.getCurrency(),
                e.getDescription(), e.getStatus(), e.getSubmitterId(), submitterName,
                e.getDepartmentId(), departmentName, hasReceipt, e.getCreatedAt(), e.getUpdatedAt());
    }
}
