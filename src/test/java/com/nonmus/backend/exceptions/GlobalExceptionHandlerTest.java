package com.nonmus.backend.exceptions;

import com.nonmus.backend.modules.auth.dto.response.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleInvalidVerificationCodeException_ReturnsBadRequest() {
        InvalidVerificationCodeException exception = new InvalidVerificationCodeException("Invalid verification code");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleInvalidVerificationCodeException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("INVALID_VERIFICATION_CODE");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid verification code");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleVerifyAttemptsExceededException_ReturnsTooManyRequests() {
        VerifyAttemptsExceededException exception = new VerifyAttemptsExceededException("Maximum verification attempts exceeded");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleVerifyAttemptsExceededException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(429);
        assertThat(response.getBody().getError()).isEqualTo("VERIFY_ATTEMPTS_EXCEEDED");
        assertThat(response.getBody().getMessage()).isEqualTo("Maximum verification attempts exceeded");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleUserAlreadyExistsException_ReturnsConflict() {
        UserAlreadyExistsException exception = new UserAlreadyExistsException("A user with this email already exists");

        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleUserAlreadyExistsException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("USER_ALREADY_EXISTS");
        assertThat(response.getBody().getMessage()).isEqualTo("A user with this email already exists");
    }
}
