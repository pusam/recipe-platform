package com.recipeplatform.backend.security;

import com.recipeplatform.backend.entity.User;
import com.recipeplatform.backend.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Long userId = tokenProvider.parseUserId(token);
                Optional<User> user = userRepository.findById(userId);
                if (user.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserPrincipal principal = new UserPrincipal(user.get());
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else if (user.isEmpty()) {
                    log.warn("JWT 유효하지만 사용자 미존재: userId={}, path={}", userId, request.getRequestURI());
                }
            } catch (ExpiredJwtException e) {
                log.debug("만료된 JWT: path={}", request.getRequestURI());
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("잘못된 JWT: path={}, reason={}", request.getRequestURI(), e.getMessage());
            } catch (Exception e) {
                log.error("JWT 처리 중 예기치 못한 오류: path={}", request.getRequestURI(), e);
            }
        }
        chain.doFilter(request, response);
    }
}
