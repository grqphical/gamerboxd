package com.gamerboxd.gamerboxd.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.repositories.ReviewRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    public List<Review> getAll() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Review> getAllByUserId(int userId) {
        return reviewRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    private void validate(String gameName, int rating, String description) {
        if (gameName == null || gameName.isBlank()) {
            throw new IllegalArgumentException("Game name is required.");
        }
        validateContent(rating, description);
    }

    private void validateContent(int rating, String description) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Review description is required.");
        }
    }

    /** Validates and saves a review. Throws IllegalArgumentException if invalid. */
    public Review create(int userId, String gameName, int rating, String description) {
        validate(gameName, rating, description);
        User user = userRepository.getReferenceById(userId);
        return reviewRepository.save(new Review(user, gameName.trim(), rating, description.trim()));
    }

    public Review getOwnedReview(int reviewId, int userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found."));
        if (review.getUser().getId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own reviews.");
        }
        return review;
    }

    public Review update(int reviewId, int userId, int rating, String description) {
        Review review = getOwnedReview(reviewId, userId);
        validateContent(rating, description);
        review.setRating(rating);
        review.setDescription(description.trim());
        return reviewRepository.save(review);
    }
}
