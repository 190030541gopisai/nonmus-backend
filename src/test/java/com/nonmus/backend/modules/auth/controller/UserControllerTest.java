package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.exceptions.GlobalExceptionHandler;
import com.nonmus.backend.modules.auth.service.UserService;
import com.nonmus.backend.modules.auth.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private RateLimitService rateLimitService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(userService, rateLimitService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void isUsernameAvailable_whenRateLimitExceeded_returns429TooManyRequests() throws Exception {
        when(rateLimitService.tryConsume(any())).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/username/available")
                        .param("username", "john_doe")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void isUsernameAvailable_whenAvailable_returns200WithTrue() throws Exception {
        when(rateLimitService.tryConsume(any())).thenReturn(true);
        when(userService.isUsernameAvailable("john_doe")).thenReturn(true);

        mockMvc.perform(get("/api/v1/users/username/available")
                        .param("username", "john_doe")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void isUsernameAvailable_whenTaken_returns200WithFalse() throws Exception {
        when(rateLimitService.tryConsume(any())).thenReturn(true);
        when(userService.isUsernameAvailable("existing_user")).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/username/available")
                        .param("username", "existing_user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("existing_user"))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void isUsernameAvailable_whenTakenDifferentCase_returnsFalse() throws Exception {
        when(rateLimitService.tryConsume(any())).thenReturn(true);
        when(userService.isUsernameAvailable("Existing_User")).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/username/available")
                        .param("username", "Existing_User")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void isUsernameAvailable_whenTooShort_returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/username/available")
                        .param("username", "ab")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void isUsernameAvailable_whenInvalidCharacters_returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/username/available")
                        .param("username", "invalid#name")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void isUsernameAvailable_whenBlank_returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users/username/available")
                        .param("username", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}