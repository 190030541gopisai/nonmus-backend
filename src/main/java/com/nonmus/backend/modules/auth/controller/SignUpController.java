package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.modules.auth.dto.request.SignUpRequest;
import com.nonmus.backend.modules.auth.dto.response.SignUpResponse;
import com.nonmus.backend.modules.auth.service.JwtCookieFactory;
import com.nonmus.backend.modules.auth.service.SignUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SignUpController {

    private final SignUpService signUpService;
    private final JwtCookieFactory jwtCookieFactory;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(
            @Valid @RequestBody SignUpRequest request,
            @CookieValue(name = "${verification.token.cookie-name:email_verification_token}") String token) {

        log.info("Received sign up request for username: {}", request.getUsername());

        var result = signUpService.signUp(request, token);

        SignUpResponse response = SignUpResponse.builder()
            .message("User signed up successfully")
            .username(result.getUser().getUsername())
            .email(result.getUser().getEmail())
            .avatar(result.getUser().getAvatar())
            .build();

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        for (ResponseCookie cookie : jwtCookieFactory.createAuthCookies(result.getUser())) {
            responseBuilder.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return responseBuilder.body(response);
    }
}
