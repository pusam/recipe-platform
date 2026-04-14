package com.recipeplatform.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:change-me-please-use-at-least-32-characters-secret-key}")
    private String secret;

    @Value("${app.jwt.expiration-ms:604800000}")
    private long expirationMs;

    private SecretKey key;

    private static final String DEV_DEFAULT_SECRET =
            "dev-only-default-secret-please-override-app.jwt.secret-in-production-env";

    @PostConstruct
    public void init() {
        String effective = (secret == null || secret.isBlank()) ? DEV_DEFAULT_SECRET : secret;
        byte[] bytes = effective.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(Long userId, String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
}
