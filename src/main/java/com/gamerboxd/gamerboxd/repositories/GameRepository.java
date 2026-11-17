package com.gamerboxd.gamerboxd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamerboxd.gamerboxd.models.Game;

public interface GameRepository extends JpaRepository<Game, Integer> {
    // Games that have a cover, most-rated first
    List<Game> findTop36ByCoverLinkNotNullOrderByNumRatingsGamerboxdDescIdAsc();
}