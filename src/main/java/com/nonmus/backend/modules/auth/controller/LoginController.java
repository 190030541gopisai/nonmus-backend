package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.modules.auth.dto.request.LoginRequest;
import com.nonmus.backend.modules.auth.dto.response.LoginResponse;
import com.nonmus.backend.modules.auth.entity.User;
import com.nonmus.backend.modules.auth.service.JwtCookieFactory;
import com.nonmus.backend.modules.auth.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final JwtCookieFactory jwtCookieFactory;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        log.info("Received login request for credential: {}", request.getCredential());

        User user = loginService.login(request, httpRequest);

        LoginResponse response = LoginResponse.builder()
            .message("Login successful")
            .username(user.getUsername())
            .email(user.getEmail())
            .avatar(user.getAvatar())
            .build();

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        for (ResponseCookie cookie : jwtCookieFactory.createAuthCookies(user)) {
            responseBuilder.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return responseBuilder.body(response);
    }
}