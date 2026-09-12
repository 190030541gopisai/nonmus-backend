package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.exceptions.UserAlreadyExistsException;
import com.nonmus.backend.modules.auth.dto.internal.EmailVerificationToken;
import com.nonmus.backend.modules.auth.dto.internal.ResendState;
import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeResult;
import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeState;
import com.nonmus.backend.modules.auth.dto.internal.VerifyResult;
import com.nonmus.backend.modules.auth.event.events.VerificationCodeEvent;
import com.nonmus.backend.modules.auth.event.publisher.VerificationCodeEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthEmailServiceTest {

    @Mock
    private EmailVerificationCodeService emailVerificationCodeService;

    @Mock
    private ResendRateLimitService resendRateLimitService;

    @Mock
    private UserService userService;

    @Mock
    private VerificationCodeEventPublisher eventPublisher;

    @Mock
    private EmailVerificationTokenService emailVerificationTokenService;

    private AuthEmailService authEmailService;

    @BeforeEach
    void setUp() {
        authEmailService = new AuthEmailService(
                emailVerificationCodeService,
                resendRateLimitService,
                userService,
                eventPublisher,
                emailVerificationTokenService
        );
    }

    @Test
    void sendVerificationCode_WhenUserExists_ThrowsUserAlreadyExistsException() {
        String email = "existing@example.com";
        when(userService.emailExists(email)).thenReturn(true);

        assertThatThrownBy(() -> authEmailService.sendVerificationCode(email))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("A user with this email already exists");

        verify(resendRateLimitService, never()).recordResend(any());
        verify(emailVerificationCodeService, never()).createVerificationCode(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void sendVerificationCode_WhenUserDoesNotExist_SendsCodeAndReturnsResult() {
        String email = "newuser@example.com";
        when(userService.emailExists(email)).thenReturn(false);
        when(resendRateLimitService.recordResend(email)).thenReturn(
                ResendState.builder().cooldown(60).build()
        );
        when(emailVerificationCodeService.createVerificationCode(email)).thenReturn(
                VerificationCodeState.builder().code("123456").expiryInSeconds(600).build()
        );

        VerificationCodeResult result = authEmailService.sendVerificationCode(email);

        assertThat(result).isNotNull();
        assertThat(result.getCodeExpiryInSeconds()).isEqualTo(600);
        assertThat(result.getResendInSeconds()).isEqualTo(60);
        verify(eventPublisher).publish(any(VerificationCodeEvent.class));
    }

    @Test
    void verify_WhenUserExists_ThrowsUserAlreadyExistsException() {
        String email = "existing@example.com";
        String code = "123456";
        when(userService.emailExists(email)).thenReturn(true);

        assertThatThrownBy(() -> authEmailService.verify(email, code))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("A user with this email already exists");

        verify(emailVerificationCodeService, never()).verify(any(), any());
        verify(emailVerificationTokenService, never()).createToken(any());
    }

    @Test
    void verify_WhenUserDoesNotExistAndCodeValid_ReturnsVerifyResultWithToken() {
        String email = "newuser@example.com";
        String code = "123456";
        String generatedToken = "550e8400-e29b-41d4-a716-446655440000";

        when(userService.emailExists(email)).thenReturn(false);
        when(emailVerificationTokenService.createToken(email)).thenReturn(
                EmailVerificationToken.builder().email(email).token(generatedToken).build()
        );

        VerifyResult result = authEmailService.verify(email, code);

        assertThat(result).isNotNull();
        assertThat(result.isVerified()).isTrue();
        assertThat(result.getVerificationToken()).isEqualTo(generatedToken);

        verify(emailVerificationCodeService).verify(email, code);
        verify(emailVerificationTokenService).createToken(email);
    }
}
