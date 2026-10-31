package com.gamerboxd.gamerboxd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamerboxd.gamerboxd.models.Review;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    // Get all reviews in order of createdAt (newest first)
    List<Review> findAllByOrderByCreatedAtDesc();

    // Get a user's reviews in order of createdAt (newest first)
    List<Review> findAllByUserIdOrderByCreatedAtDesc(int userId);
}
