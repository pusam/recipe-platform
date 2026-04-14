package com.recipeplatform.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDtos {

    @Data
    public static class SignupRequest {
        @Email @NotBlank
        private String email;

        @NotBlank @Size(min = 2, max = 100)
        private String username;

        @NotBlank @Size(min = 6, max = 100)
        private String password;
    }

    @Data
    public static class LoginRequest {
        @Email @NotBlank
        private String email;

        @NotBlank
        private String password;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private Long userId;
        private String email;
        private String username;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String email;
        private String username;
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank @Size(min = 2, max = 100)
        private String username;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank @Size(min = 6, max = 100)
        private String newPassword;
    }
}
