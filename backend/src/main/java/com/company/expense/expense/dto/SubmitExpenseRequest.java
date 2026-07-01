package com.company.expense.expense.dto;

import com.company.expense.expense.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Form fields for an expense submission (sent alongside the receipt file part). */
public record SubmitExpenseRequest(
        @NotNull Category category,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than 0") BigDecimal amount,
        @Size(max = 3) String currency,
        @NotNull @Size(min = 1, max = 1000) String description) {
}
