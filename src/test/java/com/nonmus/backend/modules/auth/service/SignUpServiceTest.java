package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.modules.auth.dto.internal.EmailVerificationToken;
import com.nonmus.backend.modules.auth.dto.request.SignUpRequest;
import com.nonmus.backend.modules.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignUpServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private EmailVerificationTokenService emailVerificationTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SignUpService signUpService;

    @Test
    void signUp_success() {
        var req = SignUpRequest.builder()
            .firstName("A").lastName("B").username("user1").password("pwd").confirmPassword("pwd")
            .build();
        when(emailVerificationTokenService.getByToken("tok")).thenReturn(
            EmailVerificationToken.builder().email("a@b.com").token("tok").build()
        );
        when(emailVerificationTokenService.consumeToken("tok")).thenReturn(
            EmailVerificationToken.builder().email("a@b.com").token("tok").build()
        );
        when(userService.emailExists("a@b.com")).thenReturn(false);
        when(userService.isUsernameAvailable("user1")).thenReturn(true);
        when(passwordEncoder.encode("pwd")).thenReturn("hashed");
        when(userService.createUser(any(User.class))).thenAnswer(i -> i.getArgument(0));

        var res = signUpService.signUp(req, "tok");

        assertEquals("a@b.com", res.getUser().getEmail());
        assertEquals("hashed", res.getUser().getPasswordHash());
    }

    @Test
    void signUp_ThrowsInvalidPasswordException() {
        var req = SignUpRequest.builder()
            .firstName("A").lastName("B").username("user1").password("pwd").confirmPassword("wrong")
            .build();
        assertThrows(com.nonmus.backend.exceptions.InvalidPasswordException.class, () -> {
            signUpService.signUp(req, "tok");
        });
    }

    @Test
    void signUp_ThrowsInvalidVerificationTokenException() {
        var req = SignUpRequest.builder()
            .firstName("A").lastName("B").username("user1").password("pwd").confirmPassword("pwd")
            .build();
        when(emailVerificationTokenService.getByToken("bdtok")).thenReturn(null);
        assertThrows(com.nonmus.backend.exceptions.InvalidVerificationTokenException.class, () -> {
            signUpService.signUp(req, "bdtok");
        });
    }
}
