package com.company.expense.auth;

import com.company.expense.user.Role;
import com.company.expense.user.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Authenticated principal placed in the SecurityContext. */
public class AppUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final Long departmentId;
    private final boolean enabled;

    public AppUserPrincipal(Long id, String email, String passwordHash, Role role,
                            Long departmentId, boolean enabled) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.departmentId = departmentId;
        this.enabled = enabled;
    }

    public static AppUserPrincipal from(User user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(),
                user.getRole(), user.getDepartmentId(), user.isEnabled());
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
