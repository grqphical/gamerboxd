package com.gamerboxd.gamerboxd.services;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@Service
public class UserService implements UserDetailsService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    // BCrypt can't hash anything longer
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final int MAX_EMAIL_LENGTH = 254;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> getUserById(int id) {
        return userRepository.findById(id);
    }

    /** Validates and saves a new user. Throws IllegalArgumentException if invalid. */
    public User register(String username, String email, String password) {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanEmail = normalizeEmail(email);
        validate(cleanUsername, cleanEmail, password);

        try {
            return userRepository.save(new User(0, cleanUsername, cleanEmail, passwordEncoder.encode(password),
                    UserRole.USER));
        } catch (DataIntegrityViolationException e) {
            // Lost a race with another sign-up using the same username or email
            throw new IllegalArgumentException("That username or email is already taken.");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(normalizeEmail(email))
                .map(AuthenticatedUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void validate(String username, String email, String password) {
        if (!username.matches("[A-Za-z0-9_-]{3,30}")) {
            throw new IllegalArgumentException(
                    "Username must be 3-30 characters using only letters, numbers, underscores, and hyphens.");
        }
        if (email.length() > MAX_EMAIL_LENGTH || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (password == null || password.isBlank() || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Password must be at most " + MAX_PASSWORD_BYTES + " bytes.");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("That username is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("That email is already registered.");
        }
    }

    /** Same as `validate(...)` but with only one IllegalArgumentException being thrown */
    public void validateWithLongException(String username, String email, String password) {
        String buffer = "";

        if (username == null || !username.matches("[A-Za-z0-9_-]{3,30}")) {
            buffer += "- Username must be 3-30 characters using only letters, numbers, underscores, and hyphens.\n";
        }
        if (email == null || email.length() > MAX_EMAIL_LENGTH || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            buffer += "- Enter a valid email address.\n";
        }

        if (password == null || password.isBlank() || password.length() < MIN_PASSWORD_LENGTH) {
            buffer += "- Password must be at least " + MIN_PASSWORD_LENGTH + " characters.\n";
        }
        else if(password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            buffer += "- Password must be at most " + MAX_PASSWORD_BYTES + " bytes.\n";
        }

        if (buffer.isBlank()) {
            if (userRepository.existsByUsernameIgnoreCase(username)) {
                buffer += "- That username is already taken.\n";
            }
            if (userRepository.existsByEmail(email)) {
                buffer += "- That email is already registered.\n";
            }  
        }

        if (!buffer.isBlank()) {
            throw new IllegalArgumentException(buffer);
        }
    }
}
