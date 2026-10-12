package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.exceptions.InvalidPasswordException;
import com.nonmus.backend.exceptions.InvalidVerificationTokenException;
import com.nonmus.backend.exceptions.UserNotFoundException;
import com.nonmus.backend.modules.auth.dto.internal.EmailVerificationToken;
import com.nonmus.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.nonmus.backend.modules.auth.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private EmailVerificationTokenService emailVerificationTokenService;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordService passwordService;

    @Test
    void resetPassword_throwsInvalidPasswordException_whenPasswordsDoNotMatch() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("Valid@12")
                .confirmPassword("Different@12")
                .build();

        assertThrows(InvalidPasswordException.class,
                () -> passwordService.resetPassword("verification-token", request));

        verifyNoInteractions(emailVerificationTokenService);
    }

    @Test
    void resetPassword_throwsInvalidVerificationTokenException_whenTokenIsInvalidOrExpired() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("Valid@12")
                .confirmPassword("Valid@12")
                .build();
        when(emailVerificationTokenService.consumeToken("verification-token")).thenReturn(null);

        assertThrows(InvalidVerificationTokenException.class,
                () -> passwordService.resetPassword("verification-token", request));

        verify(emailVerificationTokenService).consumeToken("verification-token");
    }

    @Test
    void resetPassword_throwsUserNotFoundException_whenVerifiedUserDoesNotExist() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("Valid@12")
                .confirmPassword("Valid@12")
                .build();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .email("missing@example.com")
                .token("verification-token")
                .build();
        when(emailVerificationTokenService.consumeToken("verification-token")).thenReturn(token);
        when(userService.findByEmail(token.getEmail())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> passwordService.resetPassword("verification-token", request));

        verify(userService).findByEmail(token.getEmail());
    }

    @Test
    void resetPassword_hashesAndSavesPassword_whenTokenAndUserAreValid() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("Valid@12")
                .confirmPassword("Valid@12")
                .build();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .email("user@example.com")
                .token("verification-token")
                .build();
        User user = User.builder().email(token.getEmail()).passwordHash("old-hash").build();
        when(emailVerificationTokenService.consumeToken("verification-token")).thenReturn(token);
        when(userService.findByEmail(token.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("new-hash");

        passwordService.resetPassword("verification-token", request);

        verify(passwordEncoder).encode(request.getPassword());
        assertEquals("new-hash", user.getPasswordHash());
        verify(userService).updateUser(user);
    }
}
