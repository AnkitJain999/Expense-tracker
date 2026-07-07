package com.company.expense.expense;

import com.company.expense.approval.ApprovalAction;
import com.company.expense.approval.ApprovalEvent;
import com.company.expense.approval.ApprovalEventRepository;
import com.company.expense.auth.AppUserPrincipal;
import com.company.expense.common.ApiException;
import com.company.expense.department.Department;
import com.company.expense.department.DepartmentRepository;
import com.company.expense.expense.dto.ApprovalEventResponse;
import com.company.expense.expense.dto.ExpenseDetailResponse;
import com.company.expense.expense.dto.ExpenseResponse;
import com.company.expense.expense.dto.SubmitExpenseRequest;
import com.company.expense.notification.EmailNotificationService;
import com.company.expense.receipt.Receipt;
import com.company.expense.receipt.ReceiptRepository;
import com.company.expense.user.Role;
import com.company.expense.user.User;
import com.company.expense.user.UserRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExpenseService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/jpg", "application/pdf");
    private static final long MAX_RECEIPT_BYTES = 5L * 1024 * 1024;

    private final ExpenseRepository expenseRepository;
    private final ReceiptRepository receiptRepository;
    private final ApprovalEventRepository approvalEventRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmailNotificationService notifications;

    public ExpenseService(ExpenseRepository expenseRepository,
                          ReceiptRepository receiptRepository,
                          ApprovalEventRepository approvalEventRepository,
                          UserRepository userRepository,
                          DepartmentRepository departmentRepository,
                          EmailNotificationService notifications) {
        this.expenseRepository = expenseRepository;
        this.receiptRepository = receiptRepository;
        this.approvalEventRepository = approvalEventRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.notifications = notifications;
    }

    @Transactional
    public ExpenseResponse submit(AppUserPrincipal principal, SubmitExpenseRequest request,
                                  MultipartFile receiptFile) {
        Long departmentId = principal.getDepartmentId();
        if (departmentId == null) {
            throw ApiException.badRequest("You are not assigned to a department and cannot submit expenses");
        }
        String currency = (request.currency() == null || request.currency().isBlank())
                ? "USD" : request.currency().toUpperCase();

        Expense expense = new Expense(principal.getId(), departmentId, request.category(),
                request.amount(), currency, request.description());
        // A Team Lead can't approve their own expense, so their submissions skip straight to Finance.
        if (principal.getRole() == Role.TEAM_LEAD) {
            expense.setStatus(ExpenseStatus.PENDING_FINANCE);
        }
        expense = expenseRepository.save(expense);

        if (receiptFile != null && !receiptFile.isEmpty()) {
            saveReceipt(expense.getId(), receiptFile);
        }

        approvalEventRepository.save(new ApprovalEvent(
                expense.getId(), principal.getId(), ApprovalAction.SUBMITTED, null));
        if (expense.getStatus() == ExpenseStatus.PENDING_FINANCE) {
            notifications.notifySubmittedDirectToFinance(expense);
        } else {
            notifications.notifySubmitted(expense);
        }

        return toResponse(expense);
    }

    private void saveReceipt(Long expenseId, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw ApiException.badRequest("Unsupported receipt type; allowed: PNG, JPEG, PDF");
        }
        if (file.getSize() > MAX_RECEIPT_BYTES) {
            throw ApiException.badRequest("Receipt exceeds the 5MB size limit");
        }
        try {
            Receipt receipt = new Receipt(expenseId, file.getOriginalFilename(), contentType,
                    file.getSize(), file.getBytes());
            receiptRepository.save(receipt);
        } catch (IOException ex) {
            throw ApiException.badRequest("Could not read the uploaded receipt");
        }
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listMine(AppUserPrincipal principal) {
        List<Expense> expenses = expenseRepository.findBySubmitterIdOrderByCreatedAtDesc(principal.getId());
        return toResponses(expenses);
    }

    @Transactional(readOnly = true)
    public ExpenseDetailResponse getDetail(AppUserPrincipal principal, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> ApiException.notFound("Expense not found"));
        authorizeView(principal, expense);

        List<ApprovalEvent> events = approvalEventRepository.findByExpenseIdOrderByCreatedAtAsc(expenseId);
        Map<Long, String> names = userNames(events.stream().map(ApprovalEvent::getActorId).toList());
        List<ApprovalEventResponse> history = events.stream()
                .map(e -> ApprovalEventResponse.of(e, names.getOrDefault(e.getActorId(), "Unknown")))
                .toList();

        return new ExpenseDetailResponse(toResponse(expense), history);
    }

    /** Owner, the department's Team Lead, or the department's Finance Manager may view. */
    public void authorizeView(AppUserPrincipal principal, Expense expense) {
        if (expense.getSubmitterId().equals(principal.getId())) {
            return;
        }
        Department dept = departmentRepository.findById(expense.getDepartmentId()).orElse(null);
        if (dept != null) {
            if (principal.getRole() == Role.TEAM_LEAD && principal.getId().equals(dept.getTeamLeadId())) {
                return;
            }
            if (principal.getRole() == Role.FINANCE_MANAGER
                    && principal.getId().equals(dept.getFinanceManagerId())) {
                return;
            }
        }
        throw ApiException.forbidden("You are not allowed to view this expense");
    }

    // --- mapping helpers ---

    private ExpenseResponse toResponse(Expense expense) {
        return toResponses(List.of(expense)).get(0);
    }

    public List<ExpenseResponse> toResponses(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return List.of();
        }
        Map<Long, String> userNames = userNames(expenses.stream().map(Expense::getSubmitterId).toList());
        Map<Long, String> deptNames = departmentRepository
                .findAllById(expenses.stream().map(Expense::getDepartmentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Department::getId, Department::getName));
        Set<Long> withReceipts = new HashSet<>();
        for (Expense e : expenses) {
            if (receiptRepository.existsByExpenseId(e.getId())) {
                withReceipts.add(e.getId());
            }
        }
        return expenses.stream()
                .map(e -> ExpenseResponse.of(e,
                        userNames.getOrDefault(e.getSubmitterId(), "Unknown"),
                        deptNames.getOrDefault(e.getDepartmentId(), "Unknown"),
                        withReceipts.contains(e.getId())))
                .toList();
    }

    private Map<Long, String> userNames(List<Long> userIds) {
        return userRepository.findAllById(new HashSet<>(userIds)).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
    }
}
