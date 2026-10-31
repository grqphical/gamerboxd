package com.gamerboxd.gamerboxd.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;

        User u = new User();
        u.setUsername("grqphical");
        u.setEmail("test@testing.com");

        this.userRepository.save(u);
    }

    public Optional<User> getUserById(int id) {
        return userRepository.findById(id);
    }
}
