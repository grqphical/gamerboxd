package com.gamerboxd.gamerboxd.configurations;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

// Sign-up doesn't exist yet, so the dev profile seeds a user to sign in with
@Configuration
@Profile("dev")
public class DevDataConfig {
    private static final String DEV_EMAIL = "dev@gamerboxd.local";

    @Bean
    public CommandLineRunner seedDevUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail(DEV_EMAIL).isEmpty()) {
                userRepository.save(new User(0, "dev", DEV_EMAIL, passwordEncoder.encode("password"), UserRole.USER));
            }
        };
    }
}
