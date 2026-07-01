package com.company.expense.approval.dto;

import jakarta.validation.constraints.Size;

/** Body for approve/reject actions. Comment is optional on approve, required on reject. */
public record ApprovalDecisionRequest(
        @Size(max = 1000) String comment) {
}
