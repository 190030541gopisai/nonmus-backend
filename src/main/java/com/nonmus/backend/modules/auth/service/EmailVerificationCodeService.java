package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.exceptions.InvalidVerificationCodeException;
import com.nonmus.backend.exceptions.VerifyAttemptsExceededException;
import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeState;
import com.nonmus.backend.modules.auth.repository.EmailVerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationCodeService {
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Value("${verification.code.max-attempts:5}")
    private int maxVerifyAttempts;

    @Value("${verification.code.ttl:10m}")
    private Duration verificationCodeTtl;

    public VerificationCodeState createVerificationCode(String email) {
        log.debug("Creating verification code for email: {}", email);

        String verificationCode = generateCode();

        VerificationCodeState state = VerificationCodeState.builder()
                .code(verificationCode)
                .usedVerifyAttempts(0)
                .maxVerifyAttempts(maxVerifyAttempts)
                .expiryInSeconds(verificationCodeTtl.toSeconds())
                .build();

        emailVerificationCodeRepository.save(email, state);
        log.info("Verification code created and saved for email: {}", email);

        return state;
    }

    public void verify(String email, String code) {
        log.debug("Verifying code for email: {}", email);

        long result = emailVerificationCodeRepository.verifyCode(email, code);

        if (result == EmailVerificationCodeRepository.VERIFY_NOT_FOUND) {
            log.warn("No verification code found or expired for email: {}", email);
            throw new InvalidVerificationCodeException("Expired verification code");
        }

        if (result == EmailVerificationCodeRepository.VERIFY_ATTEMPTS_EXCEEDED) {
            log.warn("Max verification attempts exceeded for email: {}", email);
            throw new VerifyAttemptsExceededException("Maximum verification attempts exceeded. Please request a new code.");
        }

        if (result == EmailVerificationCodeRepository.VERIFY_SUCCESS) {
            log.info("Verification code matched for email: {}", email);
            return;
        }

        log.warn("Verification code mismatch for email: {}. {} attempt(s) remaining.", email, result);
        throw new InvalidVerificationCodeException("Invalid verification code.");
    }

    private String generateCode() {
        return String.format(
                "%0" + CODE_LENGTH + "d",
                RANDOM.nextInt(1_000_000)
        );
    }
}
