package com.company.expense.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "approval_events")
public class ApprovalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalAction action;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ApprovalEvent() {
    }

    public ApprovalEvent(Long expenseId, Long actorId, ApprovalAction action, String comment) {
        this.expenseId = expenseId;
        this.actorId = actorId;
        this.action = action;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public Long getExpenseId() {
        return expenseId;
    }

    public Long getActorId() {
        return actorId;
    }

    public ApprovalAction getAction() {
        return action;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
