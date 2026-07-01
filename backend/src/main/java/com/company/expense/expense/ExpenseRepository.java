package com.company.expense.expense;

import com.company.expense.dashboard.PendingByDepartmentRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findBySubmitterIdOrderByCreatedAtDesc(Long submitterId);

    List<Expense> findByStatusAndDepartmentIdInOrderByCreatedAtAsc(
            ExpenseStatus status, List<Long> departmentIds);

    /**
     * Aggregates pending expense amounts grouped by department. Only expenses still
     * in flight ({@code PENDING_TEAM_LEAD} or {@code PENDING_FINANCE}) are counted.
     */
    @Query("""
            SELECT new com.company.expense.dashboard.PendingByDepartmentRow(
                       d.id, d.name,
                       COALESCE(SUM(e.amount), 0),
                       COUNT(e),
                       COALESCE(SUM(CASE WHEN e.status = com.company.expense.expense.ExpenseStatus.PENDING_TEAM_LEAD THEN e.amount ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN e.status = com.company.expense.expense.ExpenseStatus.PENDING_FINANCE THEN e.amount ELSE 0 END), 0))
            FROM Department d
            LEFT JOIN Expense e
                   ON e.departmentId = d.id
                  AND e.status IN (com.company.expense.expense.ExpenseStatus.PENDING_TEAM_LEAD,
                                   com.company.expense.expense.ExpenseStatus.PENDING_FINANCE)
            GROUP BY d.id, d.name
            ORDER BY d.name
            """)
    List<PendingByDepartmentRow> aggregatePendingByDepartment();

    @Query("""
            SELECT new com.company.expense.dashboard.PendingByDepartmentRow(
                       d.id, d.name,
                       COALESCE(SUM(e.amount), 0),
                       COUNT(e),
                       COALESCE(SUM(CASE WHEN e.status = com.company.expense.expense.ExpenseStatus.PENDING_TEAM_LEAD THEN e.amount ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN e.status = com.company.expense.expense.ExpenseStatus.PENDING_FINANCE THEN e.amount ELSE 0 END), 0))
            FROM Department d
            LEFT JOIN Expense e
                   ON e.departmentId = d.id
                  AND e.status = :status
            WHERE d.id IN :departmentIds
            GROUP BY d.id, d.name
            ORDER BY d.name
            """)
    List<PendingByDepartmentRow> aggregatePendingByDepartmentForStatus(
            @Param("status") ExpenseStatus status,
            @Param("departmentIds") List<Long> departmentIds);
}
