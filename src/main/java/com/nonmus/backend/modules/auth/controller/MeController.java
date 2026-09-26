package com.nonmus.backend.modules.auth.controller;

import com.nonmus.backend.exceptions.UserNotFoundException;
import com.nonmus.backend.modules.auth.dto.response.MeResponse;
import com.nonmus.backend.modules.auth.entity.User;
import com.nonmus.backend.modules.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal String email) {
        log.info("Fetching profile for email: {}", email);

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user no longer exists"));

        return MeResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .avatar(user.getAvatar())
            .createdAt(user.getCreatedAt())
            .build();
    }
}