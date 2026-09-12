package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.modules.auth.dto.internal.EmailVerificationToken;
import com.nonmus.backend.modules.auth.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    private Duration tokenTtl;

    private EmailVerificationTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenTtl = Duration.ofMinutes(15);
        tokenService = new EmailVerificationTokenService(tokenRepository, tokenTtl);
    }

    @Test
    void createToken_GeneratesSecureTokenAndSavesInRedis() {
        String email = "user@example.com";

        EmailVerificationToken result = tokenService.createToken(email);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getToken()).isNotBlank();

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(tokenRepository).save(tokenCaptor.capture(), eq(email), eq(tokenTtl));
        assertThat(result.getToken()).isEqualTo(tokenCaptor.getValue());
    }

    @Test
    void getByToken_WhenTokenExists_ReturnsTokenDto() {
        String token = "valid-token";
        String email = "user@example.com";
        when(tokenRepository.findEmailByToken(token)).thenReturn(Optional.of(email));

        EmailVerificationToken result = tokenService.getByToken(token);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getToken()).isEqualTo(token);
    }

    @Test
    void getByToken_WhenTokenNotFound_ReturnsNull() {
        String token = "expired-or-invalid-token";
        when(tokenRepository.findEmailByToken(token)).thenReturn(Optional.empty());

        EmailVerificationToken result = tokenService.getByToken(token);

        assertThat(result).isNull();
    }

    @Test
    void consumeToken_WhenTokenExists_DeletesAndReturnsToken() {
        String token = "valid-token";
        String email = "user@example.com";
        when(tokenRepository.getAndDelete(token)).thenReturn(Optional.of(email));

        EmailVerificationToken result = tokenService.consumeToken(token);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getToken()).isEqualTo(token);
        verify(tokenRepository).getAndDelete(token);
    }

    @Test
    void consumeToken_WhenTokenDoesNotExist_ReturnsNull() {
        String token = "missing-token";
        when(tokenRepository.getAndDelete(token)).thenReturn(Optional.empty());

        EmailVerificationToken result = tokenService.consumeToken(token);

        assertThat(result).isNull();
        verify(tokenRepository).getAndDelete(token);
    }
}
