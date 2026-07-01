package com.company.expense.auth.dto;

public record AuthResponse(
        String token,
        long expiresInMinutes,
        UserProfile user) {
}
