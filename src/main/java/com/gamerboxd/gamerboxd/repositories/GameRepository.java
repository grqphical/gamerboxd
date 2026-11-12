package com.gamerboxd.gamerboxd.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamerboxd.gamerboxd.models.Game;

public interface GameRepository extends JpaRepository<Game, Integer> {
}