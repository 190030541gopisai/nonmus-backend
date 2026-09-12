package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.exceptions.InvalidVerificationCodeException;
import com.nonmus.backend.exceptions.VerifyAttemptsExceededException;
import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeState;
import com.nonmus.backend.modules.auth.repository.EmailVerificationCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationCodeServiceTest {

    @Mock
    private EmailVerificationCodeRepository codeRepository;

    private EmailVerificationCodeService codeService;

    @BeforeEach
    void setUp() {
        codeService = new EmailVerificationCodeService(codeRepository);
        ReflectionTestUtils.setField(codeService, "maxVerifyAttempts", 5);
        ReflectionTestUtils.setField(codeService, "verificationCodeTtl", Duration.ofMinutes(10));
    }

    @Test
    void createVerificationCode_GeneratesAndSavesState() {
        String email = "test@example.com";

        VerificationCodeState state = codeService.createVerificationCode(email);

        assertThat(state).isNotNull();
        assertThat(state.getCode()).hasSize(6).containsOnlyDigits();
        assertThat(state.getUsedVerifyAttempts()).isZero();
        assertThat(state.getMaxVerifyAttempts()).isEqualTo(5);
        assertThat(state.getExpiryInSeconds()).isEqualTo(600);
        verify(codeRepository).save(email, state);
    }

    @Test
    void verify_WhenCodeMatches_DoesNotThrowException() {
        String email = "test@example.com";
        String code = "123456";
        when(codeRepository.verifyCode(email, code)).thenReturn(EmailVerificationCodeRepository.VERIFY_SUCCESS);

        codeService.verify(email, code);

        verify(codeRepository).verifyCode(email, code);
    }

    @Test
    void verify_WhenStateNotFound_ThrowsInvalidVerificationCodeException() {
        String email = "test@example.com";
        when(codeRepository.verifyCode(email, "123456")).thenReturn(EmailVerificationCodeRepository.VERIFY_NOT_FOUND);

        assertThatThrownBy(() -> codeService.verify(email, "123456"))
                .isInstanceOf(InvalidVerificationCodeException.class)
                .hasMessageContaining("Expired verification code");
    }

    @Test
    void verify_WhenAttemptsExceeded_ThrowsVerifyAttemptsExceededException() {
        String email = "test@example.com";
        when(codeRepository.verifyCode(email, "123456")).thenReturn(EmailVerificationCodeRepository.VERIFY_ATTEMPTS_EXCEEDED);

        assertThatThrownBy(() -> codeService.verify(email, "123456"))
                .isInstanceOf(VerifyAttemptsExceededException.class)
                .hasMessageContaining("Maximum verification attempts exceeded");
    }

    @Test
    void verify_WhenCodeMismatchesAndUnderMaxAttempts_ThrowsInvalidVerificationCodeException() {
        String email = "test@example.com";
        when(codeRepository.verifyCode(email, "999999")).thenReturn(3L);

        assertThatThrownBy(() -> codeService.verify(email, "999999"))
                .isInstanceOf(InvalidVerificationCodeException.class)
                .hasMessageContaining("Invalid verification code");
    }
}
