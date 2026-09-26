package com.nonmus.backend.exceptions;

import com.nonmus.backend.modules.auth.dto.internal.ApiFieldError;
import com.nonmus.backend.modules.auth.dto.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        log.warn("Validation error on request: {}", exception.getMessage());

        List<ApiFieldError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiFieldError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Request validation failed")
                .timestamp(Instant.now())
                .fieldErrors(errors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception) {
        log.warn("Constraint violation: {}", exception.getMessage());

        List<ApiFieldError> errors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ApiFieldError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Request validation failed")
                .timestamp(Instant.now())
                .fieldErrors(errors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidVerificationCodeException(
            InvalidVerificationCodeException exception) {
        log.warn("Invalid verification code: {}", exception.getMessage());
        return buildSimpleResponse(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", exception.getMessage());
    }

    @ExceptionHandler(VerifyAttemptsExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleVerifyAttemptsExceededException(
            VerifyAttemptsExceededException exception) {
        log.warn("Verification attempts exceeded: {}", exception.getMessage());
        return buildSimpleResponse(HttpStatus.TOO_MANY_REQUESTS, "VERIFY_ATTEMPTS_EXCEEDED", exception.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExistsException(
            UserAlreadyExistsException exception) {
        log.warn("User already exists conflict: {}", exception.getMessage());
        return buildSimpleResponse(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(ResendCooldownActiveException.class)
    public ResponseEntity<ApiErrorResponse> handleResendCooldownActiveException(
            ResendCooldownActiveException exception) {
        log.warn("Resend cooldown active: {}, retryAfter: {}s", exception.getMessage(), exception.getRetryAfterSeconds());

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("RESEND_COOLDOWN_ACTIVE")
                .message(exception.getMessage())
                .retryAfterSeconds(exception.getRetryAfterSeconds())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()))
                .body(response);
    }

    @ExceptionHandler(ResendAttemptsExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleResendAttemptsExceededException(
            ResendAttemptsExceededException exception) {
        log.warn("Resend attempts exceeded: {}", exception.getMessage());
        return buildSimpleResponse(HttpStatus.TOO_MANY_REQUESTS, "RESEND_ATTEMPTS_EXCEEDED", exception.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException exception) {
        log.warn("Rate limit exceeded: {}", exception.getMessage());
        return buildSimpleResponse(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentialsException(
            InvalidCredentialsException exception) {
        log.warn("Invalid credentials: {}", exception.getMessage());
        return buildSimpleResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountLockedException(
            AccountLockedException exception) {
        log.warn("Account locked: {}", exception.getMessage());
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.LOCKED.value())
                .error("ACCOUNT_LOCKED")
                .message(exception.getMessage())
                .retryAfterSeconds(exception.getRetryAfterSeconds())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.LOCKED).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFoundException(
            UserNotFoundException exception) {
        log.warn("User not found: {}", exception.getMessage());
        return buildSimpleResponse(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPasswordException(
            InvalidPasswordException exception) {
        log.warn("Invalid password (mismatch): {}", exception.getMessage());
        return buildSimpleResponse(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", exception.getMessage());
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidVerificationTokenException(
            InvalidVerificationTokenException exception) {
        log.warn("Invalid verification token: {}", exception.getMessage());
        return buildSimpleResponse(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_TOKEN", exception.getMessage());
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestCookieException(
            MissingRequestCookieException exception) {
        log.warn("Missing request cookie: {}", exception.getMessage());
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("MISSING_COOKIE")
                .message("Missing required cookie: " + exception.getCookieName())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleInternalServerError(
            Exception exception) {
        log.error("Unhandled exception caught in global handler", exception);

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private ResponseEntity<ApiErrorResponse> buildSimpleResponse(HttpStatus status, String errorCode, String message) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(status.value())
                .error(errorCode)
                .message(message)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}