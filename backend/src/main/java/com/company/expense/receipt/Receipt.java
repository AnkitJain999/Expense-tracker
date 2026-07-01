package com.company.expense.receipt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false, unique = true)
    private Long expenseId;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Lob
    @Column(name = "data", nullable = false)
    private byte[] data;

    protected Receipt() {
    }

    public Receipt(Long expenseId, String filename, String contentType, long sizeBytes, byte[] data) {
        this.expenseId = expenseId;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public Long getExpenseId() {
        return expenseId;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public byte[] getData() {
        return data;
    }
}
