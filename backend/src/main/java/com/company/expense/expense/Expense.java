package com.company.expense.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submitter_id", nullable = false)
    private Long submitterId;

    /** Department the expense is routed through, captured at submit time. */
    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExpenseStatus status = ExpenseStatus.PENDING_TEAM_LEAD;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Expense() {
    }

    public Expense(Long submitterId, Long departmentId, Category category, BigDecimal amount,
                   String currency, String description) {
        this.submitterId = submitterId;
        this.departmentId = departmentId;
        this.category = category;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSubmitterId() {
        return submitterId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public ExpenseStatus getStatus() {
        return status;
    }

    public void setStatus(ExpenseStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
