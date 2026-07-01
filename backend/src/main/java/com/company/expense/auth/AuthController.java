package com.company.expense.auth;

import com.company.expense.auth.dto.AuthResponse;
import com.company.expense.auth.dto.LoginRequest;
import com.company.expense.auth.dto.UserProfile;
import com.company.expense.common.ApiException;
import com.company.expense.common.SecurityUtils;
import com.company.expense.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserProfile me() {
        Long id = SecurityUtils.currentUser().getId();
        return userRepository.findById(id)
                .map(UserProfile::from)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
