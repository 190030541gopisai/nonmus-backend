package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeResult;
import com.nonmus.backend.modules.auth.dto.request.SendEmailRequest;
import com.nonmus.backend.modules.auth.dto.request.VerifyEmailRequest;
import com.nonmus.backend.modules.auth.dto.response.VerificationCodeResponse;
import com.nonmus.backend.modules.auth.dto.response.VerifyResponse;
import com.nonmus.backend.modules.auth.service.AuthEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthEmailController {
    private final AuthEmailService authEmailService;

    @PostMapping("/email")
    public ResponseEntity<VerificationCodeResponse> sendVerificationCode(@Valid @RequestBody SendEmailRequest request) {
        VerificationCodeResult verificationCodeResult = authEmailService.sendVerificationCode(request.getEmail());

        VerificationCodeResponse verificationCodeResponse = VerificationCodeResponse.builder()
                .codeExpiryInSeconds(verificationCodeResult.getCodeExpiryInSeconds())
                .resendInSeconds(verificationCodeResult.getResendInSeconds())
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(verificationCodeResponse);
    }

    @PostMapping("/email/verify")
    public VerifyResponse verifyEmail(@RequestBody VerifyEmailRequest request) {
        return null;
    }
}