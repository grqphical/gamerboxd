package com.gamerboxd.gamerboxd.repositories;

import java.util.Optional;

import com.gamerboxd.gamerboxd.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsBySteamID(String steamId);

    Optional<User> findBySteamID(String steamId);
}
