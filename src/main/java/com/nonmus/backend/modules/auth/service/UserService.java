package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        log.debug("Checking if email exists: {}", email);
        return userRepository.existsByEmail(email);
    }

    @Cacheable(value = "usernameAvailability", condition = "#username != null", key = "#username.trim().toLowerCase()", unless = "#result == true")
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        String target = username.trim();
        boolean exists = userRepository.existsByUsernameIgnoreCase(target);
        return !exists;
    }
}