package com.gamerboxd.gamerboxd.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.GameStatus;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserPrivacy;
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

    @Test
    void registerWithGoogle_savesLinkedUserWithUnknownPassword() {
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        userService.registerWithGoogle("  ada  ", " Ada@Gmail.COM ", "google-123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("ada", saved.getUsername());
        assertEquals("ada@gmail.com", saved.getEmail());
        assertEquals("google-123", saved.getGoogleID());
        assertEquals("hashed", saved.getPassword());
        assertEquals(UserRole.USER, saved.getRole());
    }

    @Test
    void linkGoogleAccount_ownedByAnotherUser_throws() {
        User other = new User(2, "bob", "bob@gmail.com", "pw", UserRole.USER);
        other.setGoogleID("google-123");
        when(userRepository.findByGoogleID("google-123")).thenReturn(Optional.of(other));

        assertThrows(IllegalArgumentException.class, () -> userService.linkGoogleAccount("google-123", 1));
        verify(userRepository, never()).save(any());
    }
    private User john() {
        return new User(1, "john", "john@gmail.com", "old-hash", UserRole.USER);
    }

    @Test
    void toggleRole_flipsBetweenUserAndAdmin() {
        User john = john();
        when(userRepository.findById(1)).thenReturn(Optional.of(john));

        userService.toggleRole(1);
        assertEquals(UserRole.ADMIN, john.getRole());

        userService.toggleRole(1);
        assertEquals(UserRole.USER, john.getRole());
    }

    @Test
    void toggleRole_unknownUser_throwsNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.toggleRole(99));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void editDescription_unknownUser_throwsNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.editDescription(99, "Hi"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateSettings_savesNormalizedValues() {
        User john = john();

        userService.updateSettings(john, " johnny ", " Johnny@Gmail.COM ", "", "PRIVATE", "Hi", "he/him");

        verify(userRepository).save(john);
        assertEquals("johnny", john.getUsername());
        assertEquals("johnny@gmail.com", john.getEmail());
        assertEquals(UserPrivacy.PRIVATE, john.getPrivacy());
        assertEquals("Hi", john.getDescription());
        assertEquals("he/him", john.getPronouns());
    }

    @Test
    void updateSettings_blankPassword_keepsCurrentPassword() {
        User john = john();

        userService.updateSettings(john, "john", "john@gmail.com", "", "public", "", "");

        assertEquals("old-hash", john.getPassword());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateSettings_newPassword_isHashed() {
        User john = john();
        when(passwordEncoder.encode(PASSWORD)).thenReturn("new-hash");

        userService.updateSettings(john, "john", "john@gmail.com", PASSWORD, "public", "", "");

        assertEquals("new-hash", john.getPassword());
    }

    @Test
    void updateSettings_unknownPrivacy_defaultsToPublic() {
        User john = john();
        john.setPrivacy(UserPrivacy.PRIVATE);

        userService.updateSettings(john, "john", "john@gmail.com", "", null, "", "");

        assertEquals(UserPrivacy.PUBLIC, john.getPrivacy());
    }

    @Test
    void updateSettings_keepingOwnUsernameAndEmail_skipsTakenChecks() {
        User john = john();

        userService.updateSettings(john, "JOHN", "john@gmail.com", "", "public", "", "");

        verify(userRepository, never()).existsByUsernameIgnoreCase(any());
        verify(userRepository, never()).existsByEmail(any());
        assertEquals("JOHN", john.getUsername());
    }

    @Test
    void updateSettings_takenUsernameOrEmail_throwsAndChangesNothing() {
        User john = john();
        when(userRepository.existsByUsernameIgnoreCase("olivia")).thenReturn(true);
        when(userRepository.existsByEmail("olivia@gmail.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateSettings(john, "olivia", "olivia@gmail.com", "", "public", "", ""));

        assertEquals("- That username is already taken.\n- That email is already registered.\n", ex.getMessage());
        assertEquals("john", john.getUsername());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateSettings_reportsEveryInvalidField() {
        User john = john();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateSettings(john, "x", "not-an-email", "short",
                        "public", "d".repeat(User.MAX_DESCRIPTION_LENGTH + 1), "he-him"));

        assertEquals(5, ex.getMessage().lines().count());
        assertEquals("john", john.getUsername());
        assertEquals("old-hash", john.getPassword());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateSettings_lostRaceOnUniqueConstraint_throwsFriendlyError() {
        User john = john();
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateSettings(john, "johnny", "john@gmail.com", "", "public", "", ""));
    }

    @Test
    void setGameStatuses_appliesStatusToEveryGame() {
        User john = john();
        when(userRepository.findById(1)).thenReturn(Optional.of(john));

        userService.setGameStatuses(1, List.of(7, 9), GameStatus.PLAYED);

        assertEquals(Map.of(7, GameStatus.PLAYED, 9, GameStatus.PLAYED), john.getGameStatuses());
    }
}
