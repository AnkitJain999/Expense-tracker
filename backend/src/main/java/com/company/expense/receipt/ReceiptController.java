package com.company.expense.receipt;

import com.company.expense.common.ApiException;
import com.company.expense.common.SecurityUtils;
import com.company.expense.expense.Expense;
import com.company.expense.expense.ExpenseRepository;
import com.company.expense.expense.ExpenseService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/receipts")
public class ReceiptController {

    private final ReceiptRepository receiptRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseService expenseService;

    public ReceiptController(ReceiptRepository receiptRepository,
                             ExpenseRepository expenseRepository,
                             ExpenseService expenseService) {
        this.receiptRepository = receiptRepository;
        this.expenseRepository = expenseRepository;
        this.expenseService = expenseService;
    }

    /** Streams the receipt image/PDF bytes. Only the owner or an approver may access it. */
    @GetMapping("/{expenseId}")
    public ResponseEntity<Resource> stream(@PathVariable Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> ApiException.notFound("Expense not found"));
        expenseService.authorizeView(SecurityUtils.currentUser(), expense);

        Receipt receipt = receiptRepository.findByExpenseId(expenseId)
                .orElseThrow(() -> ApiException.notFound("No receipt for this expense"));

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(receipt.getFilename() == null ? "receipt" : receipt.getFilename())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(receipt.getContentType()))
                .contentLength(receipt.getSizeBytes())
                .body(new ByteArrayResource(receipt.getData()));
    }
}
