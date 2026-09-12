package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public boolean isUsernameAlreadyTaken(String username) {
        log.debug("Checking if username is taken: {}", username);
        return userRepository.existsByUsername(username);
    }
}
