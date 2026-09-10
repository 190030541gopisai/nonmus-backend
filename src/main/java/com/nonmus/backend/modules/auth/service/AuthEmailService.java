package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.exceptions.UserAlreadyExistsException;
import com.nonmus.backend.modules.auth.dto.internal.ResendState;
import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeResult;
import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeState;
import com.nonmus.backend.modules.auth.dto.internal.VerifyResult;
import com.nonmus.backend.modules.auth.event.events.VerificationCodeEvent;
import com.nonmus.backend.modules.auth.event.publisher.VerificationCodeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthEmailService {
    private final EmailVerificationCodeService emailVerificationCodeService;
    private final ResendRateLimitService resendRateLimitService;
    private final UserService userService;
    private final VerificationCodeEventPublisher eventPublisher;

    public VerificationCodeResult sendVerificationCode(String email) {
        if (userService.emailExists(email)) {
            throw new UserAlreadyExistsException("A user with this email already exists");
        }

        ResendState resendState = resendRateLimitService.recordResend(email);
        VerificationCodeState verificationCodeState = emailVerificationCodeService.createVerificationCode(email);

        VerificationCodeEvent event = VerificationCodeEvent.builder()
                .email(email)
                .code(verificationCodeState.getCode())
                .build();

        eventPublisher.publish(event);

        return VerificationCodeResult.builder()
                .codeExpiryInSeconds(verificationCodeState.getExpiryInSeconds())
                .resendInSeconds(resendState.getCooldown())
                .build();
    }

    public VerifyResult verify(String email, String code) {
        return null;
    }
}