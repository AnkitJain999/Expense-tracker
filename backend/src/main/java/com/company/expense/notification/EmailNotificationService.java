package com.company.expense.notification;

import com.company.expense.department.Department;
import com.company.expense.department.DepartmentRepository;
import com.company.expense.expense.Expense;
import com.company.expense.user.Role;
import com.company.expense.user.User;
import com.company.expense.user.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends approval-workflow notifications. Runs asynchronously so email latency
 * never blocks the request thread. In dev (app.mail.log-only=true) messages are
 * logged instead of dispatched over SMTP.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final String from;
    private final boolean enabled;
    private final boolean logOnly;

    public EmailNotificationService(
            JavaMailSender mailSender,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.enabled:true}") boolean enabled,
            @Value("${app.mail.log-only:false}") boolean logOnly) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.from = from;
        this.enabled = enabled;
        this.logOnly = logOnly;
    }

    @Async
    public void notifyAccountCreated(User user, String plainTextPassword) {
        String role = user.getRole().name().replace('_', ' ');
        String body = "Hi " + user.getFullName() + ",\n\n"
                + "Your account has been created.\n\n"
                + "Email:    " + user.getEmail() + "\n"
                + "Password: " + plainTextPassword + "\n"
                + "Role:     " + role + "\n\n"
                + "Please log in and change your password as soon as possible.";
        send(user.getEmail(), "Your account has been created", body);
    }

    @Async
    public void notifySubmitted(Expense expense) {
        approver(expense, true).ifPresent(teamLead -> send(teamLead.getEmail(),
                "New expense awaiting your approval",
                "A new expense (#" + expense.getId() + ", " + expense.getAmount() + " "
                        + expense.getCurrency() + ") was submitted and is pending your review as Team Lead."));
    }

    @Async
    public void notifySubmittedDirectToFinance(Expense expense) {
        approver(expense, false).ifPresent(financeManager -> send(financeManager.getEmail(),
                "New expense awaiting your approval",
                "A new expense (#" + expense.getId() + ", " + expense.getAmount() + " "
                        + expense.getCurrency() + ") was submitted by a Team Lead and is pending your review"
                        + " as Finance Manager."));
    }

    @Async
    public void notifyTeamLeadApproved(Expense expense) {
        approver(expense, false).ifPresent(financeManager -> send(financeManager.getEmail(),
                "Expense awaiting finance approval",
                "Expense #" + expense.getId() + " (" + expense.getAmount() + " " + expense.getCurrency()
                        + ") was approved by the Team Lead and is now pending your review as Finance Manager."));
    }

    @Async
    public void notifyDecision(Expense expense, boolean approved, String level, String comment) {
        userRepository.findById(expense.getSubmitterId()).ifPresent(submitter -> {
            String outcome = approved ? "approved" : "rejected";
            StringBuilder body = new StringBuilder("Your expense #").append(expense.getId())
                    .append(" (").append(expense.getAmount()).append(' ').append(expense.getCurrency())
                    .append(") was ").append(outcome).append(" by the ").append(level).append('.');
            if (comment != null && !comment.isBlank()) {
                body.append("\n\nComment: ").append(comment);
            }
            send(submitter.getEmail(), "Your expense was " + outcome, body.toString());
        });
    }

    private Optional<User> approver(Expense expense, boolean teamLead) {
        return departmentRepository.findById(expense.getDepartmentId())
                .map(dept -> teamLead ? dept.getTeamLeadId() : dept.getFinanceManagerId())
                .flatMap(id -> id == null ? Optional.empty() : userRepository.findById(id));
    }

    private void send(String to, String subject, String body) {
        if (!enabled) {
            return;
        }
        if (logOnly) {
            log.info("[email:log-only] to={} subject=\"{}\"\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send notification email to {}: {}", to, ex.getMessage());
        }
    }
}
