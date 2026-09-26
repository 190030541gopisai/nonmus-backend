package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.modules.auth.dto.internal.EmailVerificationToken;
import com.nonmus.backend.modules.auth.repository.EmailVerificationTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class EmailVerificationTokenService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final Duration tokenTtl;

    public EmailVerificationTokenService(
            EmailVerificationTokenRepository tokenRepository,
            @Value("${verification.token.ttl:15m}") Duration tokenTtl) {
        this.tokenRepository = tokenRepository;
        this.tokenTtl = tokenTtl;
    }

    public EmailVerificationToken createToken(String email) {
        log.debug("Creating email verification token for: {}", email);
        String token = UUID.randomUUID().toString();
        tokenRepository.save(token, email, tokenTtl);
        return EmailVerificationToken.builder()
                .token(token)
                .email(email)
                .build();
    }

    public EmailVerificationToken getByToken(String token) {
        log.debug("Retrieving email verification token");
        return tokenRepository.findEmailByToken(token)
                .map(email -> EmailVerificationToken.builder()
                        .token(token)
                        .email(email)
                        .build())
                .orElse(null);
    }

    public EmailVerificationToken consumeToken(String token) {
        log.debug("Consuming email verification token atomically");
        return tokenRepository.getAndDelete(token)
                .map(email -> EmailVerificationToken.builder()
                        .token(token)
                        .email(email)
                        .build())
                .orElse(null);
    }
}
