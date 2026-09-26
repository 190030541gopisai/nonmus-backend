package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.exceptions.InvalidPasswordException;
import com.nonmus.backend.exceptions.InvalidVerificationTokenException;
import com.nonmus.backend.exceptions.UserAlreadyExistsException;
import com.nonmus.backend.modules.auth.dto.request.SignUpRequest;
import com.nonmus.backend.modules.auth.dto.internal.SignUpResult;
import com.nonmus.backend.modules.auth.dto.internal.EmailVerificationToken;
import com.nonmus.backend.modules.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignUpService {

    private final UserService userService;
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignUpResult signUp(SignUpRequest request, String token) {
        log.info("Processing sign-up for username: {}", request.getUsername());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidPasswordException("Passwords do not match");
        }

        EmailVerificationToken verificationToken = emailVerificationTokenService.getByToken(token);
        if (verificationToken == null) {
            throw new InvalidVerificationTokenException("Invalid or expired email verification token");
        }
        String email = verificationToken.getEmail();

        if (userService.emailExists(email)) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        if (!userService.isUsernameAvailable(request.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }

        verificationToken = emailVerificationTokenService.consumeToken(token);
        if (verificationToken == null) {
            throw new InvalidVerificationTokenException("Invalid or expired email verification token");
        }

        User user = User.builder()
            .email(email)
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .username(request.getUsername())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .build();

        user = userService.createUser(user);

        return SignUpResult.builder()
            .user(user)
            .build();
    }
}
