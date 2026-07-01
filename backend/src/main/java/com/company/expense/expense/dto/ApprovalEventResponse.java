package com.company.expense.expense.dto;

import com.company.expense.approval.ApprovalAction;
import com.company.expense.approval.ApprovalEvent;
import java.time.Instant;

public record ApprovalEventResponse(
        Long id,
        ApprovalAction action,
        Long actorId,
        String actorName,
        String comment,
        Instant createdAt) {

    public static ApprovalEventResponse of(ApprovalEvent e, String actorName) {
        return new ApprovalEventResponse(e.getId(), e.getAction(), e.getActorId(), actorName,
                e.getComment(), e.getCreatedAt());
    }
}
