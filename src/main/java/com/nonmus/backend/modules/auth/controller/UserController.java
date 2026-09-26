package com.nonmus.backend.modules.auth.controller;
import com.nonmus.backend.modules.auth.dto.request.UsernameAvailableRequest;
import com.nonmus.backend.modules.auth.dto.response.UsernameAvailableResponse;
import com.nonmus.backend.modules.auth.service.UserService;
import com.nonmus.backend.modules.auth.service.RateLimitService;
import com.nonmus.backend.exceptions.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final String BUCKET_NAME = "username_avail";
    private final int BUCKET_CAPACITY = 20;
    private final Duration BUCKET_REFILL_PERIOD = Duration.ofMinutes(1);

    private final UserService userService;
    private final RateLimitService rateLimitService;

    @GetMapping("/username/available")
    public UsernameAvailableResponse isUsernameAvailable(@Valid UsernameAvailableRequest request, HttpServletRequest httpRequest) {
        if (!rateLimitService.tryConsume(httpRequest, BUCKET_NAME , BUCKET_CAPACITY, BUCKET_REFILL_PERIOD)) {
            throw new RateLimitExceededException("Too many requests");
        }

        log.info("Checking username availability for: {}", request.getUsername());
        boolean available = userService.isUsernameAvailable(request.getUsername());
        return UsernameAvailableResponse.builder()
                .username(request.getUsername())
                .available(available)
                .build();
    }
}