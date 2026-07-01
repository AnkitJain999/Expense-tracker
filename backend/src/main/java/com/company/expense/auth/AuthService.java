package com.company.expense.auth;

import com.company.expense.auth.dto.AuthResponse;
import com.company.expense.auth.dto.LoginRequest;
import com.company.expense.auth.dto.UserProfile;
import com.company.expense.common.ApiException;
import com.company.expense.user.User;
import com.company.expense.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            throw ApiException.badRequest("Invalid email or password");
        }
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> ApiException.badRequest("Invalid email or password"));
        String token = jwtService.generateToken(AppUserPrincipal.from(user));
        return new AuthResponse(token, jwtService.getExpirationMinutes(), UserProfile.from(user));
    }
}
