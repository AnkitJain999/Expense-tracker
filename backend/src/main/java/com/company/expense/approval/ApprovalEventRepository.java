package com.company.expense.approval;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalEventRepository extends JpaRepository<ApprovalEvent, Long> {

    List<ApprovalEvent> findByExpenseIdOrderByCreatedAtAsc(Long expenseId);
}
