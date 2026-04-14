package com.recipeplatform.backend.service;

import com.recipeplatform.backend.dto.AuthDtos;
import com.recipeplatform.backend.entity.User;
import com.recipeplatform.backend.repository.UserRepository;
import com.recipeplatform.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthDtos.AuthResponse signup(AuthDtos.SignupRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        User user = User.builder()
                .email(email)
                .username(req.getUsername().trim())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();
        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional
    public AuthDtos.UserDto updateProfile(Long userId, AuthDtos.UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setUsername(req.getUsername().trim());
        userRepository.save(user);
        return AuthDtos.UserDto.builder()
                .id(user.getId()).email(user.getEmail()).username(user.getUsername()).build();
    }

    @Transactional
    public void changePassword(Long userId, AuthDtos.ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return toAuthResponse(user);
    }

    private AuthDtos.AuthResponse toAuthResponse(User user) {
        String token = tokenProvider.createToken(user.getId(), user.getEmail());
        return AuthDtos.AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }
}
