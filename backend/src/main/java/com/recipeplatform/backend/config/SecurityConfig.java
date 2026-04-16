package com.recipeplatform.backend.config;

import com.recipeplatform.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final Environment environment;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOriginsProp;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOriginsProp.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev")
                || environment.getActiveProfiles().length == 0;

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    if (isDev) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth
                            // 공개 API
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/recipes", "/api/recipes/*").permitAll()
                            // 그 외 모든 /api 경로는 인증 필요 (기본 거부)
                            .requestMatchers("/api/**").authenticated()
                            // SPA 및 정적 리소스 등 비-API 경로는 허용
                            .anyRequest().permitAll();
                })
                .headers(h -> {
                    if (isDev) {
                        h.frameOptions(f -> f.sameOrigin());
                    }
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
