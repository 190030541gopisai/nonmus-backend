package com.nonmus.backend.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonmus.backend.modules.auth.dto.internal.SignUpResult;
import com.nonmus.backend.modules.auth.dto.request.SignUpRequest;
import com.nonmus.backend.modules.auth.entity.User;
import com.nonmus.backend.modules.auth.service.JwtCookieFactory;
import com.nonmus.backend.modules.auth.service.JwtService;
import com.nonmus.backend.modules.auth.service.SignUpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SignUpController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit test
public class SignUpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SignUpService signUpService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtCookieFactory jwtCookieFactory;


    @Test
    public void signUp_Success_SetsJwtCookies() throws Exception {
        SignUpRequest request = new SignUpRequest();
        request.setUsername("testuser");
        request.setPassword("password123#");
        request.setConfirmPassword("password123#");
        request.setFirstName("Test");
        request.setLastName("User");

        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();
        SignUpResult result = SignUpResult.builder().user(user).build();

        MockCookie verificationCookie = new MockCookie("email_verification_token", "verifTokenValue");

        ResponseCookie accessCookie = ResponseCookie.from("access_token", "access123").path("/").build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "refresh123").path("/").build();

        when(signUpService.signUp(any(SignUpRequest.class), eq("verifTokenValue"))).thenReturn(result);
        when(jwtCookieFactory.createAuthCookies(any(User.class))).thenReturn(List.of(accessCookie, refreshCookie));

        mockMvc.perform(post("/api/v1/auth/signup")
                .cookie(verificationCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User signed up successfully"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().value("access_token", "access123"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().value("refresh_token", "refresh123"));
    }

    @Test
    public void signUp_MissingCookie_ReturnsBadRequest() throws Exception {
        SignUpRequest request = new SignUpRequest();
        request.setUsername("testuser");
        request.setPassword("password123#");
        request.setConfirmPassword("password123#");
        request.setFirstName("Test");
        request.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/signup")
                // no cookie
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_COOKIE"));
    }

    @Test
    public void signUp_MissingFields_ReturnsBadRequest() throws Exception {
        SignUpRequest request = new SignUpRequest(); // empty payload
        MockCookie verificationCookie = new MockCookie("email_verification_token", "verifTokenValue");

        mockMvc.perform(post("/api/v1/auth/signup")
                .cookie(verificationCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
