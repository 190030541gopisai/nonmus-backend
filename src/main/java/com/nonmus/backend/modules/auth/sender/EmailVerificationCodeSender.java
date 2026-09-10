package com.nonmus.backend.modules.auth.sender;

import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeSendResult;
import com.nonmus.backend.modules.auth.dto.request.VerificationCodeTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCodeSender
        implements VerificationCodeSender {

    private static final String SUBJECT = "Nonmus Verification Code";

    private static final String CONTENT = """
            Your Nonmus verification code is: %s

            This code will expire in %d minutes. Please do not share this code with anyone.
            """;

    private final MailSender mailSender;

    @Value("${verification.code.ttl:10m}")
    private Duration verificationCodeTtl = Duration.ofMinutes(10);

    @Value("${spring.mail.username:noreply@nonmus.com}")
    private String fromAddress;

    @Override
    public VerificationCodeSendResult send(
            VerificationCodeTarget target
    ) {
        log.debug("Sending verification code email to {}", target.getEmail());
        long expiryMinutes = Math.max(1, verificationCodeTtl.toMinutes());
        String content = CONTENT.formatted(target.getCode(), expiryMinutes);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(target.getEmail());
        message.setSubject(SUBJECT);
        message.setText(content);

        try {
            mailSender.send(message);
            log.info("Verification code email sent successfully to {}", target.getEmail());

            return VerificationCodeSendResult.builder()
                    .sent(true)
                    .build();
        } catch (MailException e) {
            log.error("Failed to send verification code email to {}", target.getEmail(), e);
            throw e;
        }
    }
}