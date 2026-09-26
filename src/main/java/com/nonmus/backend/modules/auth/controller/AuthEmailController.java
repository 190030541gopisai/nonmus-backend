package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeResult;
import com.nonmus.backend.modules.auth.dto.internal.VerifyResult;
import com.nonmus.backend.modules.auth.dto.request.SendEmailRequest;
import com.nonmus.backend.modules.auth.dto.request.VerifyEmailRequest;
import com.nonmus.backend.modules.auth.dto.response.VerificationCodeResponse;
import com.nonmus.backend.modules.auth.dto.response.VerifyResponse;
import com.nonmus.backend.modules.auth.service.AuthEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthEmailController {
    private final AuthEmailService authEmailService;

    @Value("${verification.token.ttl:15m}")
    private Duration tokenTtl;

    @Value("${verification.token.cookie-name:email_verification_token}")
    private String tokenCookieName;

    @Value("${verification.token.cookie-secure:false}")
    private boolean isSecureCookie;

    @PostMapping("/email")
    public ResponseEntity<VerificationCodeResponse> sendVerificationCode(@Valid @RequestBody SendEmailRequest request) {
        log.info("Received request to send verification code to email: {}", request.getEmail());

        VerificationCodeResult verificationCodeResult = authEmailService.sendVerificationCode(request.getEmail());

        VerificationCodeResponse verificationCodeResponse = VerificationCodeResponse.builder()
                .codeExpiryInSeconds(verificationCodeResult.getCodeExpiryInSeconds())
                .resendInSeconds(verificationCodeResult.getResendInSeconds())
                .build();

        log.info("Verification code sent successfully to email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(verificationCodeResponse);
    }

    @PostMapping("/email/verify")
    public ResponseEntity<VerifyResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        log.info("Received request to verify email: {}", request.getEmail());

        VerifyResult verifyResult = authEmailService.verify(request.getEmail(), request.getCode());

        ResponseCookie cookie = ResponseCookie.from(tokenCookieName, verifyResult.getVerificationToken())
                .httpOnly(true)
                .secure(isSecureCookie)
                .path("/api/v1/auth")
                .maxAge(tokenTtl)
                .sameSite("Lax")
                .build();

        VerifyResponse response = VerifyResponse.builder()
                .verified(verifyResult.isVerified())
                .build();

        log.info("Email verified successfully for: {}", request.getEmail());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}