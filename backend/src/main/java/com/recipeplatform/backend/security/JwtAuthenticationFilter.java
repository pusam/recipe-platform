package com.recipeplatform.backend.security;

import com.recipeplatform.backend.entity.User;
import com.recipeplatform.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
                }
            } catch (Exception ignored) {
                // invalid token → unauthenticated request
            }
        }
        chain.doFilter(request, response);
    }
}
