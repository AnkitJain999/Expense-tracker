package com.company.expense.receipt;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByExpenseId(Long expenseId);

    boolean existsByExpenseId(Long expenseId);
}
