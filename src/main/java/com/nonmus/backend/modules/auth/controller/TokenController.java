package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.modules.auth.entity.User;
import com.nonmus.backend.modules.auth.service.JwtCookieFactory;
import com.nonmus.backend.modules.auth.service.JwtService;
import com.nonmus.backend.modules.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class TokenController {

    private final JwtService jwtService;
    private final UserService userService;
    private final JwtCookieFactory jwtCookieFactory;

    @GetMapping("/refresh")
    public ResponseEntity<Void> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        if (jwtService.isExpired(refreshToken)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        String email = jwtService.getEmailFromAccessToken(refreshToken);

        if (!StringUtils.hasText(email)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        String accessToken = jwtService.generateAccessToken(userOpt.get());

        ResponseCookie accessTokenCookie = jwtCookieFactory.createAccessTokenCookie(accessToken);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .build();
    }
}
