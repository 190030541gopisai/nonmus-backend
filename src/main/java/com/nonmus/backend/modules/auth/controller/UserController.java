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

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RateLimitService rateLimitService;

    @GetMapping("/username/available")
    public UsernameAvailableResponse isUsernameAvailable(@Valid UsernameAvailableRequest request, HttpServletRequest httpRequest) {
        if (!rateLimitService.tryConsume(httpRequest)) {
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