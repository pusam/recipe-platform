package com.recipeplatform.backend.controller;

import com.recipeplatform.backend.dto.AuthDtos;
import com.recipeplatform.backend.security.UserPrincipal;
import com.recipeplatform.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/auth/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody AuthDtos.SignupRequest req) {
        AuthDtos.AuthResponse res = userService.signup(req);
        return ResponseEntity.ok(Map.of("success", true, "data", res));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        AuthDtos.AuthResponse res = userService.login(req);
        return ResponseEntity.ok(Map.of("success", true, "data", res));
    }

    @GetMapping("/users/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "인증이 필요합니다."));
        }
        AuthDtos.UserDto dto = AuthDtos.UserDto.builder()
                .id(principal.getId())
                .email(principal.getEmail())
                .username(principal.getUser().getUsername())
                .build();
        return ResponseEntity.ok(Map.of("success", true, "data", dto));
    }

    @PutMapping("/users/me")
    public ResponseEntity<Map<String, Object>> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                             @Valid @RequestBody AuthDtos.UpdateProfileRequest req) {
        AuthDtos.UserDto dto = userService.updateProfile(principal.getId(), req);
        return ResponseEntity.ok(Map.of("success", true, "data", dto));
    }

    @PutMapping("/users/me/password")
    public ResponseEntity<Map<String, Object>> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                              @Valid @RequestBody AuthDtos.ChangePasswordRequest req) {
        userService.changePassword(principal.getId(), req);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
