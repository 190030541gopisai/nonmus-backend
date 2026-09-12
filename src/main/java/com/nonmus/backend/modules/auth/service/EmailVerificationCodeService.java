package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeState;
import com.nonmus.backend.modules.auth.dto.internal.VerifyResult;
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

    public VerifyResult verify(String email, String code) {
        return null;
    }

    private String generateCode() {
        return String.format(
                "%0" + CODE_LENGTH + "d",
                RANDOM.nextInt(1_000_000)
        );
    }
}
