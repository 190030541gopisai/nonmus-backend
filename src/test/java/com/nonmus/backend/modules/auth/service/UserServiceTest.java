package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.modules.auth.entity.User;
import com.nonmus.backend.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Setup a simple in-memory cache manager for testing
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(new ConcurrentMapCache("usernameAvailability")));
        this.cacheManager = cacheManager;

        userService = new UserService(userRepository);
    }

    @Test
    void updateUser_doesNotSave_whenUserDoesNotExist() {
        User user = User.builder().id(UUID.randomUUID()).build();
        when(userRepository.existsById(user.getId())).thenReturn(false);

        userService.updateUser(user);

        verify(userRepository, never()).save(user);
    }

    @Test
    void isUsernameAvailable_whenNotExists_returnsTrue() {
        // Arrange
        String username = "new_user";
        when(userRepository.existsByUsernameIgnoreCase(username)).thenReturn(false);

        // Act
        boolean available = userService.isUsernameAvailable(username);

        // Assert
        assertThat(available).isTrue();
        verify(userRepository).existsByUsernameIgnoreCase(username);
    }

    @Test
    void isUsernameAvailable_whenExistsExactCase_returnsFalse() {
        // Arrange
        String username = "existing_user";
        when(userRepository.existsByUsernameIgnoreCase(username)).thenReturn(true);

        // Act
        boolean available = userService.isUsernameAvailable(username);

        // Assert
        assertThat(available).isFalse();
        verify(userRepository).existsByUsernameIgnoreCase(username);
    }

    @Test
    void isUsernameAvailable_whenExistsDifferentCase_returnsFalse() {
        // Arrange
        String username = "ExIsTiNg_UsEr";
        when(userRepository.existsByUsernameIgnoreCase(username)).thenReturn(true);

        // Act
        boolean available = userService.isUsernameAvailable(username);

        // Assert
        assertThat(available).isFalse();
        verify(userRepository).existsByUsernameIgnoreCase(username);
    }

    @Test
    void isUsernameAvailable_whenBlankOrNull_returnsFalse() {
        // Act & Assert
        assertThat(userService.isUsernameAvailable("")).isFalse();
        assertThat(userService.isUsernameAvailable("   ")).isFalse();
        assertThat(userService.isUsernameAvailable(null)).isFalse();
        verify(userRepository, never()).existsByUsernameIgnoreCase(any());
    }

    @Test
    void isUsernameAvailable_whenTrimmed_worksCorrectly() {
        // Arrange
        String usernameWithSpaces = "  test_user  ";
        String trimmedUsername = "test_user";
        when(userRepository.existsByUsernameIgnoreCase(trimmedUsername)).thenReturn(false);

        // Act
        boolean available = userService.isUsernameAvailable(usernameWithSpaces);

        // Assert
        assertThat(available).isTrue();
        verify(userRepository).existsByUsernameIgnoreCase(trimmedUsername);
    }
}