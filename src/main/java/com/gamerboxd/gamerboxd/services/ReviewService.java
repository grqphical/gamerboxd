package com.gamerboxd.gamerboxd.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.repositories.ReviewLikeRepository;
import com.gamerboxd.gamerboxd.repositories.ReviewRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;
import com.gamerboxd.gamerboxd.services.GameService;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final GameService gameService;

    public ReviewService(
        ReviewRepository reviewRepository, 
        UserRepository userRepository, 
        ReviewLikeRepository reviewLikeRepository,
        GameService gameService) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.gameService = gameService;
    }

    public List<Review> getAll() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Review> getAllByUserId(int userId) {
        return reviewRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    private void validate(int gameId, int rating, String description) {
        Optional<Game> game = gameService.getGameByID(gameId);
        if (!game.isPresent()) {
            throw new IllegalArgumentException("Invalid Game ID");
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
    public Review create(int userId, int gameId, int rating, String description) {
        validate(gameId, rating, description);
        User user = userRepository.getReferenceById(userId);
        return reviewRepository.save(new Review(user, gameId, rating, description.trim()));
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
        review.setUpdatedAt(Instant.now());
        return reviewRepository.save(review);
    }

    public void delete(int reviewId, int userId) {
        Review review = getOwnedReview(reviewId, userId);
        reviewLikeRepository.deleteByReviewId(reviewId);
        reviewRepository.delete(review);
    }
}
