package com.gamerboxd.gamerboxd.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.repositories.ReviewRepository;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public List<Review> getAll() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    /** Validates and saves a review. Throws IllegalArgumentException if invalid. */
    public Review create(int userId, String gameName, int rating, String description) {
        validate(gameName, rating, description);
        return reviewRepository.save(new Review(userId, gameName.trim(), rating, description.trim()));
    }

    private void validate(String gameName, int rating, String description) {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("Game name is required.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Review description is required.");
        }
    }
}
