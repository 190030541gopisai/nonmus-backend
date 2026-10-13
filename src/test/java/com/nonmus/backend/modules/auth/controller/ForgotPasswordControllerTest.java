package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.exceptions.GlobalExceptionHandler;
import com.nonmus.backend.exceptions.InvalidPasswordException;
import com.nonmus.backend.exceptions.InvalidVerificationCodeException;
import com.nonmus.backend.exceptions.InvalidVerificationTokenException;
import com.nonmus.backend.exceptions.UserNotFoundException;
import com.nonmus.backend.exceptions.VerifyAttemptsExceededException;
import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeResult;
import com.nonmus.backend.modules.auth.dto.internal.VerifyResult;
import com.nonmus.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.nonmus.backend.modules.auth.dto.request.SendEmailRequest;
import com.nonmus.backend.modules.auth.dto.request.VerifyEmailRequest;
import com.nonmus.backend.modules.auth.service.AuthEmailService;
import com.nonmus.backend.modules.auth.service.PasswordService;
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

import jakarta.servlet.http.Cookie;
import java.time.Duration;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordControllerTest {

    @Mock
    private PasswordService passwordService;

    @Mock
    private AuthEmailService authEmailService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String URL = "/api/v1/auth/forgot-password";
    private static final String SEND_CODE_URL = "/api/v1/auth/forgot-password/send-code";
    private static final String VERIFY_CODE_URL = "/api/v1/auth/forgot-password/verify-code";

    private static final Cookie VALID_COOKIE = new Cookie("email_verification_token", "some-token");

    @BeforeEach
    void setUp() {
        ForgotPasswordController controller = new ForgotPasswordController(passwordService, authEmailService);
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

    // ── POST /forgot-password (reset) ─────────────────────────────────────────

    @Test
    void forgotPassword_shouldReturn200_whenPasswordIsResetSuccessfully() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("Valid@12")
                .confirmPassword("Valid@12")
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(VALID_COOKIE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordIsBlank() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("")
                .confirmPassword("Valid@1234")
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(VALID_COOKIE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Password is required")));
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordIsNull() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password(null)
                .confirmPassword("Valid@1234")
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(VALID_COOKIE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Password is required")));
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordIsTooShort() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("Ab@1234")
                .confirmPassword("Ab@1234")
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(VALID_COOKIE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Password must be between 8 and 64 characters")));
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordIsTooLong() throws Exception {
        String tooLong = "A@1" + "a".repeat(62);
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password(tooLong)
                .confirmPassword(tooLong)
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(VALID_COOKIE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Password must be between 8 and 64 characters")));
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordHasNoDigit() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("NoDigit@!")
                .confirmPassword("NoDigit@!")
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(VALID_COOKIE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Password must contain a number")));
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordHasNoSpecialCharacter() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("NoSpecial1")
                .confirmPassword("NoSpecial1")
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(VALID_COOKIE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Password must contain a special character")));
    }

    @Test
    void forgotPassword_shouldReturn400_whenCookieIsMissing() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("Valid@12")
                .confirmPassword("Valid@12")
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_COOKIE"))
                .andExpect(jsonPath("$.message").value("Missing required cookie: email_verification_token"));
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordsDoNotMatch() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("Valid@12")
                .confirmPassword("Different@12")
                .build();

        doThrow(new InvalidPasswordException("Passwords do not match"))
                .when(passwordService).resetPassword(anyString(), any(ForgotPasswordRequest.class));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(VALID_COOKIE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }

    @Test
    void forgotPassword_shouldReturn400_whenTokenIsInvalidOrExpired() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("Valid@12")
                .confirmPassword("Valid@12")
                .build();

        doThrow(new InvalidVerificationTokenException("Invalid or expired verification token"))
                .when(passwordService).resetPassword(anyString(), any(ForgotPasswordRequest.class));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("email_verification_token", "invalid-token"))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_VERIFICATION_TOKEN"))
                .andExpect(jsonPath("$.message").value("Invalid or expired verification token"));
    }

    // ── POST /forgot-password/send-code ───────────────────────────────────────

    @Test
    void sendCode_shouldReturn202_whenUserExists() throws Exception {
        SendEmailRequest request = new SendEmailRequest("user@example.com");
        VerificationCodeResult result = VerificationCodeResult.builder()
                .codeExpiryInSeconds(600L)
                .resendInSeconds(60L)
                .build();

        when(authEmailService.sendForgotPasswordCode("user@example.com")).thenReturn(result);

        mockMvc.perform(post(SEND_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.codeExpiryInSeconds").value(600))
                .andExpect(jsonPath("$.resendInSeconds").value(60));
    }

    @Test
    void sendCode_shouldReturn401_whenUserDoesNotExist() throws Exception {
        SendEmailRequest request = new SendEmailRequest("unknown@example.com");

        when(authEmailService.sendForgotPasswordCode("unknown@example.com"))
                .thenThrow(new UserNotFoundException("No account found with this email"));

        mockMvc.perform(post(SEND_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No account found with this email"));
    }

    @Test
    void sendCode_shouldReturn400_whenEmailIsBlank() throws Exception {
        SendEmailRequest request = new SendEmailRequest("");

        mockMvc.perform(post(SEND_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Email is required")));
    }

    @Test
    void sendCode_shouldReturn400_whenEmailFormatIsInvalid() throws Exception {
        SendEmailRequest request = new SendEmailRequest("not-an-email");

        mockMvc.perform(post(SEND_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Invalid email format")));
    }

    // ── POST /forgot-password/verify-code ─────────────────────────────────────

    @Test
    void verifyCode_shouldReturn200WithCookie_whenCodeIsValid() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("user@example.com", "123456");
        String token = "550e8400-e29b-41d4-a716-446655440000";
        VerifyResult result = VerifyResult.builder()
                .verified(true)
                .verificationToken(token)
                .build();

        when(authEmailService.verifyForgotPassword("user@example.com", "123456")).thenReturn(result);

        mockMvc.perform(post(VERIFY_CODE_URL)
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
    void verifyCode_shouldReturn401_whenUserDoesNotExist() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("unknown@example.com", "123456");

        when(authEmailService.verifyForgotPassword("unknown@example.com", "123456"))
                .thenThrow(new UserNotFoundException("No account found with this email"));

        mockMvc.perform(post(VERIFY_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No account found with this email"));
    }

    @Test
    void verifyCode_shouldReturn400_whenCodeIsInvalid() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("user@example.com", "000000");

        when(authEmailService.verifyForgotPassword("user@example.com", "000000"))
                .thenThrow(new InvalidVerificationCodeException("Invalid verification code. 4 attempt(s) remaining."));

        mockMvc.perform(post(VERIFY_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_VERIFICATION_CODE"))
                .andExpect(jsonPath("$.message").value("Invalid verification code. 4 attempt(s) remaining."));
    }

    @Test
    void verifyCode_shouldReturn429_whenAttemptsExceeded() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("user@example.com", "123456");

        when(authEmailService.verifyForgotPassword("user@example.com", "123456"))
                .thenThrow(new VerifyAttemptsExceededException("Maximum verification attempts exceeded. Please request a new code."));

        mockMvc.perform(post(VERIFY_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("VERIFY_ATTEMPTS_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("Maximum verification attempts exceeded. Please request a new code."));
    }

    @Test
    void verifyCode_shouldReturn400_whenEmailFormatIsInvalid() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("invalid-email", "123456");

        mockMvc.perform(post(VERIFY_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void verifyCode_shouldReturn400_whenCodeFormatIsInvalid() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest("user@example.com", "12ab");

        mockMvc.perform(post(VERIFY_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
