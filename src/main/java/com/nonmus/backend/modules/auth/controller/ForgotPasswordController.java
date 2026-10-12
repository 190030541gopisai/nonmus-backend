package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.nonmus.backend.modules.auth.dto.response.ForgotPasswordResponse;
import com.nonmus.backend.modules.auth.service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/forgot-password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final PasswordService passwordService;

    @PostMapping
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @CookieValue("email_verification_token") String emailVerificationToken,
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.resetPassword(emailVerificationToken, request);
        return ResponseEntity.ok(ForgotPasswordResponse.builder()
                .message("Password reset successful").build());
    }


    
}
