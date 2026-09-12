package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.exceptions.GlobalExceptionHandler;
import com.nonmus.backend.exceptions.InvalidVerificationCodeException;
import com.nonmus.backend.exceptions.UserAlreadyExistsException;
import com.nonmus.backend.exceptions.VerifyAttemptsExceededException;
import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeResult;
import com.nonmus.backend.modules.auth.dto.internal.VerifyResult;
import com.nonmus.backend.modules.auth.dto.request.SendEmailRequest;
import com.nonmus.backend.modules.auth.dto.request.VerifyEmailRequest;
import com.nonmus.backend.modules.auth.service.AuthEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthEmailControllerTest {

    @Mock
    private AuthEmailService authEmailService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AuthEmailController controller = new AuthEmailController(authEmailService);
        ReflectionTestUtils.setField(controller, "tokenTtl", Duration.ofMinutes(15));
        ReflectionTestUtils.setField(controller, "tokenCookieName", "email_verification_token");
        ReflectionTestUtils.setField(controller, "isSecureCookie", false);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void sendVerificationCode_WhenValidRequest_ReturnsAccepted() throws Exception {
        SendEmailRequest request = new SendEmailRequest("user@example.com");
        VerificationCodeResult result = VerificationCodeResult.builder()
                .codeExpiryInSeconds(600L)
                .resendInSeconds(60L)
                .build();

        when(authEmailService.sendVerificationCode("user@example.com")).thenReturn(result);

        mockMvc.perform(post("/api/v1/auth/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.codeExpiryInSeconds").value(600))
                .andExpect(jsonPath("$.resendInSeconds").value(60));
    }

    @Test
    void verifyEmail_WhenValidCode_ReturnsOkWithHttpOnlyCookie() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("user@example.com", "123456");
        String token = "550e8400-e29b-41d4-a716-446655440000";
        VerifyResult result = VerifyResult.builder()
                .verified(true)
                .verificationToken(token)
                .build();

        when(authEmailService.verify("user@example.com", "123456")).thenReturn(result);

        mockMvc.perform(post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("email_verification_token=" + token)))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(cookie().value("email_verification_token", token))
                .andExpect(cookie().httpOnly("email_verification_token", true))
                .andExpect(cookie().path("email_verification_token", "/api/v1/auth"));
    }

    @Test
    void verifyEmail_WhenInvalidEmailFormat_ReturnsBadRequest() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("invalid-email", "123456");

        mockMvc.perform(post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void verifyEmail_WhenInvalidCodeFormat_ReturnsBadRequest() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("user@example.com", "12ab");

        mockMvc.perform(post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void verifyEmail_WhenCodeInvalidOrExpired_ReturnsBadRequest() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("user@example.com", "123456");

        when(authEmailService.verify("user@example.com", "123456"))
                .thenThrow(new InvalidVerificationCodeException("Invalid verification code. 4 attempt(s) remaining."));

        mockMvc.perform(post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_VERIFICATION_CODE"))
                .andExpect(jsonPath("$.message").value("Invalid verification code. 4 attempt(s) remaining."));
    }

    @Test
    void verifyEmail_WhenAttemptsExceeded_ReturnsTooManyRequests() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("user@example.com", "123456");

        when(authEmailService.verify("user@example.com", "123456"))
                .thenThrow(new VerifyAttemptsExceededException("Maximum verification attempts exceeded. Please request a new code."));

        mockMvc.perform(post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("VERIFY_ATTEMPTS_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("Maximum verification attempts exceeded. Please request a new code."));
    }

    @Test
    void verifyEmail_WhenUserAlreadyExists_ReturnsConflict() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("user@example.com", "123456");

        when(authEmailService.verify("user@example.com", "123456"))
                .thenThrow(new UserAlreadyExistsException("A user with this email already exists"));

        mockMvc.perform(post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("A user with this email already exists"));
    }
}
