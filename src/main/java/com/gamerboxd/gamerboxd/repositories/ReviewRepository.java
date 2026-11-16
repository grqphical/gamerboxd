package com.gamerboxd.gamerboxd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.gamerboxd.gamerboxd.models.Review;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    // The entity graphs load each review's author in the same query instead of one query per author

    // Get all reviews in order of createdAt (newest first)
    @EntityGraph(attributePaths = "user")
    List<Review> findAllByOrderByCreatedAtDesc();

    // Get a user's reviews in order of createdAt (newest first)
    @EntityGraph(attributePaths = "user")
    List<Review> findAllByUserIdOrderByCreatedAtDesc(int userId);

    // Get a game's reviews in order of createdAt (newest first) 
    @EntityGraph(attributePaths = "user")
    List<Review> findAllByGameIdOrderByCreatedAtDesc(int gameId);

}
