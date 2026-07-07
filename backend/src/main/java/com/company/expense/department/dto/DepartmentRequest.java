package com.company.expense.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank @Size(max = 150) String name,
        Long teamLeadId,
        Long financeManagerId) {
}
