package com.recipeplatform.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:}")
    private String secret;

    @Value("${app.jwt.expiration-ms:604800000}")
    private long expirationMs;

    private final Environment environment;

    private SecretKey key;

    private static final Set<String> WEAK_DEFAULTS = Set.of(
            "please-change-this-secret-key-to-something-at-least-32-bytes-long",
            "replace-with-random-base64-at-least-32-bytes",
            "dev-only-default-secret-please-override-app.jwt.secret-in-production-env",
            "change-me-please-use-at-least-32-characters-secret-key"
    );

    public JwtTokenProvider(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        String configured = secret == null ? "" : secret.trim();

        if (configured.isEmpty() || WEAK_DEFAULTS.contains(configured)) {
            if (isProd) {
                throw new IllegalStateException(
                        "JWT_SECRET(app.jwt.secret)가 설정되지 않았거나 기본 예시값입니다. " +
                        "운영에서는 반드시 32바이트 이상의 무작위 시크릿을 설정하세요.");
            }
            this.key = generateEphemeralKey();
            log.warn("[SECURITY] JWT secret이 미설정이어서 임시 키를 생성했습니다. " +
                    "재기동 시 기존 토큰은 무효화됩니다. 영속 사용을 원하면 JWT_SECRET을 설정하세요.");
            return;
        }

        byte[] bytes = configured.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret는 최소 32바이트 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    private SecretKey generateEphemeralKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("HmacSHA256");
            kg.init(256);
            return kg.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JWT 키 생성 실패", e);
        }
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
