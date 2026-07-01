package com.company.expense.expense.dto;

import java.util.List;

public record ExpenseDetailResponse(
        ExpenseResponse expense,
        List<ApprovalEventResponse> history) {
}
