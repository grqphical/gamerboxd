package com.gamerboxd.gamerboxd.repositories;

import com.gamerboxd.gamerboxd.models.RememberMeToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RememberMeTokenRepository extends JpaRepository<RememberMeToken, String> {
    void deleteByEmail(String email);
}
