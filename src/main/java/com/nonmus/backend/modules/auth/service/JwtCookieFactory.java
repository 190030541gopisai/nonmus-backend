package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.modules.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtCookieFactory {

    private final JwtService jwtService;

    @Value("${jwt.cookie-secure:false}")
    private boolean isSecureCookie;

    public List<ResponseCookie> createAuthCookies(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        ResponseCookie accessCookie = createAccessTokenCookie(accessToken);

        ResponseCookie refreshCookie = createRefreshTokenCookie(refreshToken);

        return List.of(accessCookie, refreshCookie);
    }

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie.from("access_token", accessToken)
              .httpOnly(true)
              .secure(isSecureCookie)
              .path("/")
              .sameSite("Lax")
              .maxAge(900) // 15 minutes
              .build();
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(isSecureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(604800) // 7 days
                .build();
    }
}