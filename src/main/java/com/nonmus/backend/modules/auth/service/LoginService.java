package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.exceptions.AccountLockedException;
import com.nonmus.backend.exceptions.InvalidCredentialsException;
import com.nonmus.backend.exceptions.RateLimitExceededException;
import com.nonmus.backend.modules.auth.dto.request.LoginRequest;
import com.nonmus.backend.modules.auth.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitService rateLimitService;
    private final StringRedisTemplate stringRedisTemplate;

    private volatile String dummyHash;

    public User login(LoginRequest request, HttpServletRequest httpRequest) {
        // IP-level throttle before any expensive work.
        if (!rateLimitService.tryConsume(httpRequest, "login", 5, Duration.ofMinutes(1))) {
            throw new RateLimitExceededException("Too many attempts. Try again later.");
        }

        String credential = request.getCredential().trim();
        String counterKey = "login_failed_" + credential.toLowerCase();

        Long remainingLockout = remainingLockoutSeconds(counterKey);
        if (remainingLockout != null) {
            throw new AccountLockedException("Too many failed attempts. Try again later.", remainingLockout);
        }

        Optional<User> userOpt = userService.findByEmailOrUsername(credential);

        if (userOpt.isEmpty()) {
            // Run the same single bcrypt compare as the real path so response time does not leak account existence.
            passwordEncoder.matches(request.getPassword(), dummyHash());
            recordFailure(counterKey);
            throw new InvalidCredentialsException("Email/username or password is incorrect");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordFailure(counterKey);
            throw new InvalidCredentialsException("Email/username or password is incorrect");
        }

        clearFailures(counterKey);
        return user;
    }

    private Long remainingLockoutSeconds(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        long failures = Long.parseLong(value);
        if (failures < MAX_FAILED_ATTEMPTS) {
            return null;
        }
        Long ttl = stringRedisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : null;
    }

    private void recordFailure(String key) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, LOCKOUT_DURATION);
        } else if (count != null && count >= MAX_FAILED_ATTEMPTS) {
            // Re-arm the window once locked, regardless of when the first failure happened.
            stringRedisTemplate.expire(key, LOCKOUT_DURATION);
        }
    }

    private void clearFailures(String key) {
        stringRedisTemplate.delete(key);
    }

    /** A BCrypt hash for a throw-away value; never matches a real password. Cached so the cost matches the real path. */
    private String dummyHash() {
        String hash = dummyHash;
        if (hash == null) {
            String newHash = passwordEncoder.encode("nonmus-invalid-credential-placeholder");
            dummyHash = newHash;
            hash = newHash;
        }
        return hash;
    }
}