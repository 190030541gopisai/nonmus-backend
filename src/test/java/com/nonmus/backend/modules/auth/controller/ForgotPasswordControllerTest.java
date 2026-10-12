package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.common.BaseControllerTest;
import com.nonmus.backend.exceptions.InvalidPasswordException;
import com.nonmus.backend.exceptions.InvalidVerificationTokenException;
import com.nonmus.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.nonmus.backend.modules.auth.dto.request.SendEmailRequest;
import com.nonmus.backend.modules.auth.service.PasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ForgotPasswordController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ForgotPasswordControllerTest extends BaseControllerTest {

    @MockitoBean
    private PasswordService passwordService;

    private static final String URL = "/api/v1/auth/forgot-password";
    private static final String SEND_CODE_URL = "/api/v1/auth/forgot-password/code";

    private static final Cookie VALID_COOKIE = new Cookie("email_verification_token", "some-token");

    private void assertValidationError(ForgotPasswordRequest request, String expectedFieldMessage) throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(VALID_COOKIE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem(expectedFieldMessage)));
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordIsBlank() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("")
                .confirmPassword("Valid@1234")
                .build();

        assertValidationError(request, "Password is required");
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordIsNull() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password(null)
                .confirmPassword("Valid@1234")
                .build();

        assertValidationError(request, "Password is required");
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordIsTooShort() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("Ab@1234") // 7 chars
                .confirmPassword("Ab@1234")
                .build();

        assertValidationError(request, "Password must be between 8 and 64 characters");
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordIsTooLong() throws Exception {
        String tooLong = "A@1" + "a".repeat(62); // 65 chars
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password(tooLong)
                .confirmPassword(tooLong)
                .build();

        assertValidationError(request, "Password must be between 8 and 64 characters");
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordHasNoDigit() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("NoDigit@!")
                .confirmPassword("NoDigit@!")
                .build();

        assertValidationError(request, "Password must contain a number");
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordHasNoSpecialCharacter() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("NoSpecial1")
                .confirmPassword("NoSpecial1")
                .build();

        assertValidationError(request, "Password must contain a special character");
    }

    @Test
    void forgotPassword_shouldReturn400_whenPasswordStrengthIsWeak() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("weak")
                .confirmPassword("weak")
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(VALID_COOKIE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field", everyItem(equalTo("password"))))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void forgotPassword_shouldReturn400_whenEmailVerificationTokenCookieIsMissing() throws Exception {
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
                        .cookie(new Cookie("email_verification_token", "some-token"))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }

    @Test
    void forgotPassword_shouldReturn400_whenEmailVerificationTokenIsInvalidOrExpired() throws Exception {
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

    @Test
    void forgotPassword_shouldReturn200_whenPasswordIsResetSuccessfully() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .password("Valid@12")
                .confirmPassword("Valid@12")
                .build();


        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("email_verification_token", "valid-token"))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));
    }

    @Test
    void sendCode_shouldReturn400_whenEmailIsBlank() throws Exception {
        SendEmailRequest request = SendEmailRequest.builder()
                .email("")
                .build();

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
        SendEmailRequest request = SendEmailRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(post(SEND_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Invalid email format")));
    }

    @Test
    void sendCode_shouldReturn202_whenUserDoesNotExist() throws Exception {
        SendEmailRequest request = SendEmailRequest.builder()
                .email("nonexistent@example.com")
                .build();

        mockMvc.perform(post(SEND_CODE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("If the email exists, a verification code has been sent"));
    }
}
