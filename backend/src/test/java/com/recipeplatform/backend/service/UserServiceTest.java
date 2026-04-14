package com.recipeplatform.backend.service;

import com.recipeplatform.backend.dto.AuthDtos;
import com.recipeplatform.backend.entity.User;
import com.recipeplatform.backend.repository.UserRepository;
import com.recipeplatform.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider tokenProvider;

    @InjectMocks UserService userService;

    AuthDtos.SignupRequest signupReq;

    @BeforeEach
    void setUp() {
        signupReq = new AuthDtos.SignupRequest();
        signupReq.setEmail("  Foo@Example.com  ");
        signupReq.setUsername("foo");
        signupReq.setPassword("pw123456");
    }

    @Test
    void signup_normalizesEmail_and_hashesPassword() {
        when(userRepository.existsByEmail("foo@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pw123456")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(tokenProvider.createToken(1L, "foo@example.com")).thenReturn("TOKEN");

        AuthDtos.AuthResponse res = userService.signup(signupReq);

        assertThat(res.getToken()).isEqualTo("TOKEN");
        assertThat(res.getEmail()).isEqualTo("foo@example.com");
        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("foo@example.com")
                        && u.getPasswordHash().equals("HASH")
                        && u.getUsername().equals("foo")
        ));
    }

    @Test
    void signup_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.signup(signupReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 가입");
    }

    @Test
    void login_succeeds_withCorrectPassword() {
        User user = User.builder().id(7L).email("a@b.c").username("u").passwordHash("HASH").build();
        when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "HASH")).thenReturn(true);
        when(tokenProvider.createToken(7L, "a@b.c")).thenReturn("T");

        AuthDtos.LoginRequest req = new AuthDtos.LoginRequest();
        req.setEmail("a@b.c");
        req.setPassword("pw");

        AuthDtos.AuthResponse res = userService.login(req);
        assertThat(res.getToken()).isEqualTo("T");
        assertThat(res.getUserId()).isEqualTo(7L);
    }

    @Test
    void login_fails_withWrongPassword() {
        User user = User.builder().id(7L).email("a@b.c").username("u").passwordHash("HASH").build();
        when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        AuthDtos.LoginRequest req = new AuthDtos.LoginRequest();
        req.setEmail("a@b.c");
        req.setPassword("wrong");

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("비밀번호");
    }

    @Test
    void changePassword_rejectsWrongCurrent() {
        User user = User.builder().id(7L).passwordHash("HASH").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        AuthDtos.ChangePasswordRequest req = new AuthDtos.ChangePasswordRequest();
        req.setCurrentPassword("wrong");
        req.setNewPassword("newpass");

        assertThatThrownBy(() -> userService.changePassword(7L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 비밀번호");
    }
}
