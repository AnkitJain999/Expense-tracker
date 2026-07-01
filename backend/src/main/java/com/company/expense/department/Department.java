package com.company.expense.department;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** User id of the assigned Team Lead (first-level approver). */
    @Column(name = "team_lead_id")
    private Long teamLeadId;

    /** User id of the assigned Finance Manager (second-level approver). */
    @Column(name = "finance_manager_id")
    private Long financeManagerId;

    protected Department() {
    }

    public Department(String name, Long teamLeadId, Long financeManagerId) {
        this.name = name;
        this.teamLeadId = teamLeadId;
        this.financeManagerId = financeManagerId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTeamLeadId() {
        return teamLeadId;
    }

    public void setTeamLeadId(Long teamLeadId) {
        this.teamLeadId = teamLeadId;
    }

    public Long getFinanceManagerId() {
        return financeManagerId;
    }

    public void setFinanceManagerId(Long financeManagerId) {
        this.financeManagerId = financeManagerId;
    }
}
