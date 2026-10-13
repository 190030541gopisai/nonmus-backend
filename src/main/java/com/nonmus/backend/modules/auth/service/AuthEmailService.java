package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.exceptions.UserAlreadyExistsException;
import com.nonmus.backend.exceptions.UserNotFoundException;
import com.nonmus.backend.modules.auth.dto.internal.EmailVerificationToken;
import com.nonmus.backend.modules.auth.dto.internal.ResendState;
import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeResult;
import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeState;
import com.nonmus.backend.modules.auth.dto.internal.VerifyResult;
import com.nonmus.backend.modules.auth.event.events.VerificationCodeEvent;
import com.nonmus.backend.modules.auth.event.publisher.VerificationCodeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthEmailService {
    private final EmailVerificationCodeService emailVerificationCodeService;
    private final ResendRateLimitService resendRateLimitService;
    private final UserService userService;
    private final VerificationCodeEventPublisher eventPublisher;
    private final EmailVerificationTokenService emailVerificationTokenService;

    public VerificationCodeResult sendVerificationCode(String originalEmail) {
        String email = originalEmail.toLowerCase();
        log.info("Processing verification code request for email: {}", email);

        if (userService.emailExists(email)) {
            log.warn("User with email already exists: {}", email);
            throw new UserAlreadyExistsException("A user with this email already exists");
        }

        ResendState resendState = resendRateLimitService.recordResend(email);
        VerificationCodeState verificationCodeState = emailVerificationCodeService.createVerificationCode(email);

        VerificationCodeEvent event = VerificationCodeEvent.builder()
                .email(email)
                .code(verificationCodeState.getCode())
                .build();

        eventPublisher.publish(event);
        log.info("Verification code event published for email: {}", email);

        return VerificationCodeResult.builder()
                .codeExpiryInSeconds(verificationCodeState.getExpiryInSeconds())
                .resendInSeconds(resendState.getCooldown())
                .build();
    }

    public VerificationCodeResult sendForgotPasswordCode(String originalEmail) {
        String email = originalEmail.toLowerCase();
        log.info("Processing forgot-password verification code request for email: {}", email);

        if (!userService.emailExists(email)) {
            log.warn("Forgot-password requested for non-existent email: {}", email);
            throw new UserNotFoundException("No account found with this email");
        }

        ResendState resendState = resendRateLimitService.recordResend(email);
        VerificationCodeState verificationCodeState = emailVerificationCodeService.createVerificationCode(email);

        VerificationCodeEvent event = VerificationCodeEvent.builder()
                .email(email)
                .code(verificationCodeState.getCode())
                .build();

        eventPublisher.publish(event);
        log.info("Forgot-password verification code event published for email: {}", email);

        return VerificationCodeResult.builder()
                .codeExpiryInSeconds(verificationCodeState.getExpiryInSeconds())
                .resendInSeconds(resendState.getCooldown())
                .build();
    }

    public VerifyResult verifyForgotPassword(String originalEmail, String code) {
        String email = originalEmail.toLowerCase();
        log.info("Processing forgot-password verification for email: {}", email);

        if (!userService.emailExists(email)) {
            log.warn("Forgot-password verify for non-existent email: {}", email);
            throw new UserNotFoundException("No account found with this email");
        }

        emailVerificationCodeService.verify(email, code);

        EmailVerificationToken token = emailVerificationTokenService.createToken(email);
        log.info("Forgot-password email verified and token generated for email: {}", email);

        return VerifyResult.builder()
                .verified(true)
                .verificationToken(token.getToken())
                .build();
    }

    public VerifyResult verify(String originalEmail, String code) {
        String email = originalEmail.toLowerCase();
        log.info("Processing verification for email: {}", email);

        if (userService.emailExists(email)) {
            log.warn("User with email already exists: {}", email);
            throw new UserAlreadyExistsException("A user with this email already exists");
        }

        emailVerificationCodeService.verify(email, code);

        EmailVerificationToken token = emailVerificationTokenService.createToken(email);
        log.info("Email verification successful and token generated for email: {}", email);

        return VerifyResult.builder()
                .verified(true)
                .verificationToken(token.getToken())
                .build();
    }
}