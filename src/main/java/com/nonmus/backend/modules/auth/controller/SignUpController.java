package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.modules.auth.dto.request.SignUpRequest;
import com.nonmus.backend.modules.auth.dto.response.SignUpResponse;
import com.nonmus.backend.modules.auth.service.JwtService;
import com.nonmus.backend.modules.auth.service.SignUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SignUpController {

    private final SignUpService signUpService;
    private final JwtService jwtService;

    @Value("${jwt.cookie-secure:false}")
    private boolean isSecureCookie;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(
            @Valid @RequestBody SignUpRequest request,
            @CookieValue(name = "${verification.token.cookie-name:email_verification_token}") String token) {

        log.info("Received sign up request for username: {}", request.getUsername());

        var result = signUpService.signUp(request, token);

        String accessToken = jwtService.generateAccessToken(result.getUser());
        String refreshToken = jwtService.generateRefreshToken(result.getUser());

        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(isSecureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(900) // 15 minutes
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(isSecureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(604800) // 7 days
                .build();

        SignUpResponse response = SignUpResponse.builder()
            .message("User signed up successfully")
            .username(result.getUser().getUsername())
            .email(result.getUser().getEmail())
            .avatar(result.getUser().getAvatar())
            .build();
            
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }
}
