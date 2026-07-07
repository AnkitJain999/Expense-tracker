package com.company.expense.user.dto;

import com.company.expense.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Password is optional: leave blank/null to keep the user's current password. */
public record UpdateUserRequest(
        @NotBlank @Email String email,
        @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 255) String fullName,
        @NotNull Role role,
        Long departmentId,
        boolean enabled) {
}
