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
import com.gamerboxd.gamerboxd.models.UserPrivacy;
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

    /** Updates a user from the user repository
     *  Throws single IllegalArgumentException if any inputs are invalid */
    public void updateSettings(
        User user,
        String username, String email, String password,
        String privacy, String description, String pronouns
    ) {
        String errorMessage = "";

        final String updatedUsername = username == null ? "" : username.trim();
        if (!updatedUsername.matches("[A-Za-z0-9_-]{3,30}")) {
            errorMessage += "- Username must be 3-30 characters using only letters, numbers, underscores, and hyphens.\n";
        }
        
        final String updatedEmail = normalizeEmail(email);
        if (updatedEmail.length() > MAX_EMAIL_LENGTH || !updatedEmail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            errorMessage += "- Enter a valid email address.\n";
        }

        final boolean changingPassword = password != null && !password.isEmpty();

        if (changingPassword) {
            if (password.length() < MIN_PASSWORD_LENGTH) {
                errorMessage += "- Password must be at least " + MIN_PASSWORD_LENGTH + " characters.\n";
            }
            else if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
                errorMessage += "- Password must be at most " + MAX_PASSWORD_BYTES + " bytes.\n";
            }
        }
        
        final UserPrivacy updatedPrivacy = "private".equalsIgnoreCase(privacy)
            ? UserPrivacy.PRIVATE : UserPrivacy.PUBLIC;

        final String updatedDescription = description != null ? description : "";
        if (updatedDescription.length() > User.MAX_DESCRIPTION_LENGTH) {
            errorMessage += "- Description must be shorter than " + 
                            User.MAX_DESCRIPTION_LENGTH + " characters.\n";
        }

        final String updatedPronouns = pronouns != null ? pronouns : "";
        if (!updatedPronouns.isBlank() && !updatedPronouns.matches("[a-zA-Z]+/[a-zA-Z]+")) {
            errorMessage += "- Invalid pronoun format.\n";
        }

        if (errorMessage.isBlank()) {
            if (!updatedUsername.equalsIgnoreCase(user.getUsername()) 
                && userRepository.existsByUsernameIgnoreCase(updatedUsername)) 
            {
                errorMessage += "- That username is already taken.\n";
            }
            if (!updatedEmail.equals(user.getEmail()) 
                && userRepository.existsByEmail(updatedEmail))
            {
                errorMessage += "- That email is already registered.\n";
            }
        }

        if (!errorMessage.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        if (changingPassword) {
            user.setPassword(passwordEncoder.encode(password));
        }

        user.setUsername(updatedUsername);
        user.setEmail(updatedEmail);
        user.setPrivacy(updatedPrivacy);
        user.setDescription(updatedDescription);
        user.setPronouns(updatedPronouns);

        try {
            userRepository.save(user);
        } 
        catch (DataIntegrityViolationException e) {
            // Lost a race with another sign-up using the same username or email (copied from register)
            throw new IllegalArgumentException("- That username or email is already taken.");
        }
    }
}
