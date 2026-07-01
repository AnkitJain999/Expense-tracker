package com.company.expense.user.dto;

import com.company.expense.user.Role;
import com.company.expense.user.User;
import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        Long departmentId,
        String departmentName,
        boolean enabled,
        Instant createdAt) {

    public static UserResponse of(User user, String departmentName) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getDepartmentId(), departmentName, user.isEnabled(), user.getCreatedAt());
    }
}
