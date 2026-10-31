package com.gamerboxd.gamerboxd.repositories;

import java.util.Optional;

import com.gamerboxd.gamerboxd.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
}
