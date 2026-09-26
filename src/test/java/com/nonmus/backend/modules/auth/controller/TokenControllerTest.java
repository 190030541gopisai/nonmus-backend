package com.nonmus.backend.modules.auth.controller;


import com.nonmus.backend.modules.auth.entity.User;
import com.nonmus.backend.modules.auth.service.JwtCookieFactory;
import com.nonmus.backend.modules.auth.service.JwtService;
import com.nonmus.backend.modules.auth.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TokenController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtCookieFactory jwtCookieFactory;

    @Test
    void refreshToken_whenValid_SetAccessTokenCookie() throws Exception {
        String refreshToken = "valid-refresh-token";
        String email = "example@example.com";
        String accessToken = "access-token";
        ResponseCookie accessCookie = ResponseCookie
                .from("access_token", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .build();

        User user = new User();

        when(jwtService.getEmailFromAccessToken(refreshToken)).thenReturn(email);
        when(jwtService.isExpired(refreshToken)).thenReturn(false);
        when(userService.findByEmail(email)).thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(user)).thenReturn(accessToken);
        when(jwtCookieFactory.createAccessTokenCookie(accessToken)).thenReturn(accessCookie);

        mockMvc.perform(get("/api/v1/auth/refresh")
                .cookie(new Cookie("refresh_token", refreshToken))
        )
                .andExpect(status().isOk())
                .andExpect(cookie().value("access_token", accessToken))
                .andExpect(cookie().httpOnly("access_token", true));

        verify(jwtService).getEmailFromAccessToken(refreshToken);
        verify(jwtService).isExpired(refreshToken);
        verify(userService).findByEmail(email);
        verify(jwtService).generateAccessToken(user);
    }

    @Test
    void refreshToken_whenExpired_Return401Response() throws Exception {
        String refreshToken = "expired-refresh-token";

        when(jwtService.isExpired(refreshToken)).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshToken))
                )
                .andExpect(status().isUnauthorized());

        verify(jwtService).isExpired(refreshToken);
    }

    @Test
    void refreshToken_whenEmailIsNull_Return401Response() throws Exception {
        String refreshToken = "expired-refresh-token";

        when(jwtService.isExpired(refreshToken)).thenReturn(false);
        when(jwtService.getEmailFromAccessToken(refreshToken)).thenReturn(null);

        mockMvc.perform(get("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshToken))
                )
                .andExpect(status().isUnauthorized());

        verify(jwtService).isExpired(refreshToken);
        verify(jwtService).getEmailFromAccessToken(refreshToken);
    }

    @Test
    void refreshToken_whenUserNotFound_Return401Response() throws Exception {
        String refreshToken = "valid-refresh-token";
        String email = "example@example.com";

        when(jwtService.getEmailFromAccessToken(refreshToken))
                .thenReturn(email);

        when(jwtService.isExpired(refreshToken))
                .thenReturn(false);

        when(userService.findByEmail(email))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshToken))
                )
                .andExpect(status().isUnauthorized());

        verify(userService).findByEmail(email);
    }

    @Test
    void refreshToken_whenRefreshTokenCookieMissing_Return401Response() throws Exception {

        mockMvc.perform(get("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }
}
