package com.company.expense.config;

import com.company.expense.approval.ApprovalAction;
import com.company.expense.approval.ApprovalEvent;
import com.company.expense.approval.ApprovalEventRepository;
import com.company.expense.department.Department;
import com.company.expense.department.DepartmentRepository;
import com.company.expense.expense.Category;
import com.company.expense.expense.Expense;
import com.company.expense.expense.ExpenseRepository;
import com.company.expense.expense.ExpenseStatus;
import com.company.expense.receipt.Receipt;
import com.company.expense.receipt.ReceiptRepository;
import com.company.expense.user.Role;
import com.company.expense.user.User;
import com.company.expense.user.UserRepository;
import java.math.BigDecimal;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds demo data for local development. Runs only when app.seed.enabled=true
 * (the dev profile) and only if the users table is empty, so it is idempotent.
 *
 * All demo users share the password: {@code password123}
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    // 1x1 transparent PNG used as a placeholder receipt image.
    private static final byte[] SAMPLE_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ExpenseRepository expenseRepository;
    private final ReceiptRepository receiptRepository;
    private final ApprovalEventRepository approvalEventRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      DepartmentRepository departmentRepository,
                      ExpenseRepository expenseRepository,
                      ReceiptRepository receiptRepository,
                      ApprovalEventRepository approvalEventRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.expenseRepository = expenseRepository;
        this.receiptRepository = receiptRepository;
        this.approvalEventRepository = approvalEventRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Seeding demo data (all users password: {})", DEMO_PASSWORD);

        Department engineering = departmentRepository.save(new Department("Engineering", null, null));
        Department marketing = departmentRepository.save(new Department("Marketing", null, null));

        createUser("admin@company.com", "Ada Admin", Role.ADMIN, null);
        User fiona = createUser("fiona.finance@company.com", "Fiona Finance", Role.FINANCE_MANAGER, null);
        User bob = createUser("bob.lead@company.com", "Bob Lead", Role.TEAM_LEAD, engineering.getId());
        User dan = createUser("dan.lead@company.com", "Dan Lead", Role.TEAM_LEAD, marketing.getId());
        User alice = createUser("alice@company.com", "Alice Employee", Role.EMPLOYEE, engineering.getId());
        User carol = createUser("carol@company.com", "Carol Employee", Role.EMPLOYEE, marketing.getId());

        engineering.setTeamLeadId(bob.getId());
        engineering.setFinanceManagerId(fiona.getId());
        marketing.setTeamLeadId(dan.getId());
        marketing.setFinanceManagerId(fiona.getId());
        departmentRepository.save(engineering);
        departmentRepository.save(marketing);

        // Engineering expenses (Alice)
        createExpense(alice, engineering, Category.TRAVEL, "125.50", "Taxi to client site",
                ExpenseStatus.PENDING_TEAM_LEAD, bob, null, true);
        createExpense(alice, engineering, Category.SOFTWARE, "480.00", "JetBrains licenses",
                ExpenseStatus.PENDING_FINANCE, bob, null, true);
        createExpense(alice, engineering, Category.MEALS, "62.75", "Team lunch",
                ExpenseStatus.APPROVED, bob, fiona, false);
        createExpense(alice, engineering, Category.SUPPLIES, "899.00", "Standing desk",
                ExpenseStatus.REJECTED, bob, null, false);

        // Marketing expenses (Carol)
        createExpense(carol, marketing, Category.TRAVEL, "310.00", "Conference flight",
                ExpenseStatus.PENDING_TEAM_LEAD, dan, null, false);
        createExpense(carol, marketing, Category.OTHER, "150.00", "Booth banner printing",
                ExpenseStatus.PENDING_FINANCE, dan, null, true);

        log.info("Demo data seeded: {} users, {} departments, {} expenses",
                userRepository.count(), departmentRepository.count(), expenseRepository.count());
    }

    private User createUser(String email, String name, Role role, Long departmentId) {
        return userRepository.save(new User(email, passwordEncoder.encode(DEMO_PASSWORD), name, role, departmentId));
    }

    /**
     * Creates an expense in the given terminal/intermediate status and writes the
     * matching approval-event history so queues and audit trails look realistic.
     */
    private void createExpense(User submitter, Department dept, Category category, String amount,
                               String description, ExpenseStatus status, User teamLead, User financeManager,
                               boolean withReceipt) {
        Expense expense = new Expense(submitter.getId(), dept.getId(), category,
                new BigDecimal(amount), "USD", description);
        expense.setStatus(status);
        expense = expenseRepository.save(expense);

        if (withReceipt) {
            receiptRepository.save(new Receipt(expense.getId(), "receipt.png", "image/png",
                    SAMPLE_PNG.length, SAMPLE_PNG));
        }

        // SUBMITTED event always present.
        approvalEventRepository.save(new ApprovalEvent(expense.getId(), submitter.getId(),
                ApprovalAction.SUBMITTED, null));

        switch (status) {
            case PENDING_FINANCE -> approvalEventRepository.save(new ApprovalEvent(expense.getId(),
                    teamLead.getId(), ApprovalAction.TEAM_LEAD_APPROVED, "Looks good"));
            case APPROVED -> {
                approvalEventRepository.save(new ApprovalEvent(expense.getId(), teamLead.getId(),
                        ApprovalAction.TEAM_LEAD_APPROVED, "Approved"));
                approvalEventRepository.save(new ApprovalEvent(expense.getId(), financeManager.getId(),
                        ApprovalAction.FINANCE_APPROVED, "Reimbursed"));
            }
            case REJECTED -> approvalEventRepository.save(new ApprovalEvent(expense.getId(),
                    teamLead.getId(), ApprovalAction.TEAM_LEAD_REJECTED, "Over budget for this quarter"));
            default -> {
                // PENDING_TEAM_LEAD: only the SUBMITTED event.
            }
        }
    }
}
