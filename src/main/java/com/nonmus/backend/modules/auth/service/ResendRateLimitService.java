package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.exceptions.ResendAttemptsExceededException;
import com.nonmus.backend.exceptions.ResendCooldownActiveException;
import com.nonmus.backend.modules.auth.dto.internal.ResendState;
import com.nonmus.backend.modules.auth.repository.ResendRateLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResendRateLimitService {

    private final ResendRateLimitRepository resendRateLimitRepository;

    @Value("${resend.verification.code.cooldown:60s}")
    private Duration cooldownDuration;

    @Value("${resend.verification.code.max-attempts:5}")
    private int maxResendAttempts;

    @Value("${resend.verification.code.attempts-ttl:30m}")
    private Duration attemptsTtl;

    public ResendState recordResend(String email) {
        log.debug("Recording resend attempt for email: {}", email);

        long result = resendRateLimitRepository.recordResend(
                email,
                maxResendAttempts,
                cooldownDuration,
                attemptsTtl
        );

        if (ResendRateLimitRepository.isCooldownActive(result)) {
            long retryAfter = ResendRateLimitRepository.getRemainingCooldown(result);
            log.warn("Resend cooldown active for email: {}, retry after: {} seconds", email, retryAfter);
            throw new ResendCooldownActiveException(
                    "Resend cooldown active. Please try again in " + retryAfter + " seconds.",
                    retryAfter
            );
        }

        if (ResendRateLimitRepository.isAttemptsExceeded(result)) {
            log.warn("Maximum resend attempts exceeded for email: {}", email);
            throw new ResendAttemptsExceededException(
                    "Maximum resend attempts exceeded."
            );
        }

        log.info("Resend attempt recorded for email: {}, attempts: {}/{}", email, result, maxResendAttempts);

        return ResendState.builder()
                .cooldown(cooldownDuration.toSeconds())
                .usedResendAttempts(Math.toIntExact(result))
                .maxResendAttempts(maxResendAttempts)
                .build();
    }
}