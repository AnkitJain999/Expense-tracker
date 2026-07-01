package com.company.expense.auth.dto;

import com.company.expense.user.Role;
import com.company.expense.user.User;

public record UserProfile(
        Long id,
        String email,
        String fullName,
        Role role,
        Long departmentId) {

    public static UserProfile from(User user) {
        return new UserProfile(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), user.getDepartmentId());
    }
}
