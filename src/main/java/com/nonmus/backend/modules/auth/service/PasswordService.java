package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.exceptions.InvalidPasswordException;
import com.nonmus.backend.exceptions.InvalidVerificationTokenException;
import com.nonmus.backend.exceptions.UserNotFoundException;
import com.nonmus.backend.modules.auth.dto.internal.EmailVerificationToken;
import com.nonmus.backend.modules.auth.dto.request.ForgotPasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private final EmailVerificationTokenService emailVerificationTokenService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public void resetPassword(String emailVerificationToken, ForgotPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidPasswordException("Passwords do not match");
        }

        EmailVerificationToken token = emailVerificationTokenService.consumeToken(emailVerificationToken);
        if (token == null) {
            throw new InvalidVerificationTokenException("Invalid or expired verification token");
        }

        var user = userService.findByEmail(token.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userService.updateUser(user);
    }
}
