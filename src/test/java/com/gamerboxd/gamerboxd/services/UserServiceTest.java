package com.gamerboxd.gamerboxd.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    private static final String PASSWORD = "s3cret-passphrase";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void register_savesNormalizedUserWithHashedPassword() {
        when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed");

        userService.register("  ada  ", "  Ada@Example.COM ", PASSWORD);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("ada", saved.getUsername());
        assertEquals("ada@example.com", saved.getEmail());
        assertEquals("hashed", saved.getPassword());
        assertEquals(UserRole.USER, saved.getRole());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "ab", "has space", "bad!chars", "this-username-is-way-too-long-to-use" })
    void register_invalidUsername_throws(String username) {
        assertThrows(IllegalArgumentException.class, () -> userService.register(username, "ada@example.com", PASSWORD));
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "ada", "ada@example", "@example.com", "a b@example.com", "a@b@example.com" })
    void register_invalidEmail_throws(String email) {
        assertThrows(IllegalArgumentException.class, () -> userService.register("ada", email, PASSWORD));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordTooShort_throws() {
        assertThrows(IllegalArgumentException.class, () -> userService.register("ada", "ada@example.com", "short"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_blankPassword_throws() {
        assertThrows(IllegalArgumentException.class, () -> userService.register("ada", "ada@example.com", "         "));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordOverBcryptLimit_throws() {
        // 37 two-byte characters: under 72 characters but over 72 bytes
        String password = "é".repeat(37);

        assertThrows(IllegalArgumentException.class, () -> userService.register("ada", "ada@example.com", password));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_takenUsername_throws() {
        when(userRepository.existsByUsernameIgnoreCase("ADA")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register("ADA", "ada@example.com", PASSWORD));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_takenEmail_throws() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register("ada", "ADA@example.com", PASSWORD));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_lostRaceOnUniqueConstraint_throwsFriendlyError() {
        when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(IllegalArgumentException.class, () -> userService.register("ada", "ada@example.com", PASSWORD));
    }

    @Test
    void loadUserByUsername_normalizesEmail() {
        User user = new User(7, "ada", "ada@example.com", "hashed", UserRole.USER);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername(" Ada@Example.com ");

        assertEquals("ada@example.com", details.getUsername());
    }

    @Test
    void loadUserByUsername_unknownEmail_throws() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("nobody@example.com"));
    }
}
