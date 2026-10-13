package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeResult;
import com.nonmus.backend.modules.auth.dto.internal.VerifyResult;
import com.nonmus.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.nonmus.backend.modules.auth.dto.request.SendEmailRequest;
import com.nonmus.backend.modules.auth.dto.request.VerifyEmailRequest;
import com.nonmus.backend.modules.auth.dto.response.ForgotPasswordResponse;
import com.nonmus.backend.modules.auth.dto.response.VerificationCodeResponse;
import com.nonmus.backend.modules.auth.dto.response.VerifyResponse;
import com.nonmus.backend.modules.auth.service.AuthEmailService;
import com.nonmus.backend.modules.auth.service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/forgot-password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final PasswordService passwordService;
    private final AuthEmailService authEmailService;

    @Value("${verification.token.ttl:15m}")
    private Duration tokenTtl;

    @Value("${verification.token.cookie-name:email_verification_token}")
    private String tokenCookieName;

    @Value("${verification.token.cookie-secure:false}")
    private boolean isSecureCookie;

    @PostMapping("/send-code")
    public ResponseEntity<VerificationCodeResponse> sendCode(@Valid @RequestBody SendEmailRequest request) {
        log.info("Received forgot-password send-code request for email: {}", request.getEmail());
        VerificationCodeResult result = authEmailService.sendForgotPasswordCode(request.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(VerificationCodeResponse.builder()
                .codeExpiryInSeconds(result.getCodeExpiryInSeconds())
                .resendInSeconds(result.getResendInSeconds())
                .build());
    }

    @PostMapping("/verify-code")
    public ResponseEntity<VerifyResponse> verifyCode(@Valid @RequestBody VerifyEmailRequest request) {
        log.info("Received forgot-password verify-code request for email: {}", request.getEmail());
        VerifyResult result = authEmailService.verifyForgotPassword(request.getEmail(), request.getCode());

        ResponseCookie cookie = ResponseCookie.from(tokenCookieName, result.getVerificationToken())
                .httpOnly(true)
                .secure(isSecureCookie)
                .path("/api/v1/auth")
                .maxAge(tokenTtl)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(VerifyResponse.builder().verified(result.isVerified()).build());
    }

    @PostMapping
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @CookieValue("email_verification_token") String emailVerificationToken,
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.resetPassword(emailVerificationToken, request);
        return ResponseEntity.ok(ForgotPasswordResponse.builder()
                .message("Password reset successful").build());
    }
}
