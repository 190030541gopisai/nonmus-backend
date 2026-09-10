package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isUsernameAlreadyTaken(String username) {
        return userRepository.existsByUsername(username);
    }
}
