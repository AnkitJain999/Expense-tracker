package com.company.expense.expense;

import com.company.expense.common.SecurityUtils;
import com.company.expense.expense.dto.ExpenseDetailResponse;
import com.company.expense.expense.dto.ExpenseResponse;
import com.company.expense.expense.dto.SubmitExpenseRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /**
     * Submit an expense as multipart/form-data: a JSON "data" part (SubmitExpenseRequest)
     * plus an optional "receipt" file part.
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'TEAM_LEAD')")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse submit(
            @Valid @RequestPart("data") SubmitExpenseRequest data,
            @RequestPart(value = "receipt", required = false) MultipartFile receipt) {
        return expenseService.submit(SecurityUtils.currentUser(), data, receipt);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'TEAM_LEAD')")
    public List<ExpenseResponse> mine() {
        return expenseService.listMine(SecurityUtils.currentUser());
    }

    @GetMapping("/{id}")
    public ExpenseDetailResponse detail(@PathVariable Long id) {
        return expenseService.getDetail(SecurityUtils.currentUser(), id);
    }
}
