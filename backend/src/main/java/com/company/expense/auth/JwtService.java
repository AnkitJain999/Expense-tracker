package com.company.expense.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        byte[] keyBytes = decodeSecret(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMinutes = expirationMinutes;
    }

    private static byte[] decodeSecret(String secret) {
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Not Base64 — fall back to raw bytes below.
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(AppUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(principal.getEmail())
                .claim("uid", principal.getId())
                .claim("role", principal.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    /** Returns the subject (email) if the token is valid, otherwise throws. */
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
