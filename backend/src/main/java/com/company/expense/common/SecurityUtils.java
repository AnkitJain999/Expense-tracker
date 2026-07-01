package com.company.expense.common;

import com.company.expense.auth.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Returns the authenticated principal or throws 401-style if absent. */
    public static AppUserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return principal;
    }
}
