package com.company.expense.department.dto;

public record DepartmentResponse(
        Long id,
        String name,
        Long teamLeadId,
        String teamLeadName,
        Long financeManagerId,
        String financeManagerName) {
}
