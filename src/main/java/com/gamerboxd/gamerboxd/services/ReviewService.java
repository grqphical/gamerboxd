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
import com.gamerboxd.gamerboxd.repositories.ReviewCommentRepository;
import com.gamerboxd.gamerboxd.repositories.ReviewLikeRepository;
import com.gamerboxd.gamerboxd.repositories.ReviewRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final GameService gameService;
    private final ReviewCommentRepository reviewCommentRepository;

    public ReviewService(
        ReviewRepository reviewRepository, 
        UserRepository userRepository, 
        ReviewLikeRepository reviewLikeRepository,
        GameService gameService,
        ReviewCommentRepository reviewCommentRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.gameService = gameService;
        this.reviewCommentRepository = reviewCommentRepository;
    }

    public List<Review> getAll() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Review> getAllByUserId(int userId) {
        return reviewRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Review> getAllByGameId(int gameId) {
        return reviewRepository.findAllByGameIdOrderByCreatedAtDesc(gameId);
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
        gameService.updateGameRating(gameId, rating, true);
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
        gameService.updateGameRating(review.getGameId(), review.getRating(), false);
        review.setRating(rating);
        review.setDescription(description.trim());
        review.setUpdatedAt(Instant.now());
        gameService.updateGameRating(review.getGameId(), review.getRating(), true);
        return reviewRepository.save(review);
    }

    public Review getVisibleReview(int reviewId, Integer viewerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found."));
        User author = review.getUser();
        if (author.isPrivateAccount() && (viewerId == null || viewerId != author.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found.");
        }
        return review;
    }

    @Transactional
    public void delete(int reviewId, int userId) {
        Review review = getOwnedReview(reviewId, userId);
        gameService.updateGameRating(review.getGameId(), review.getRating(), false);
        reviewLikeRepository.deleteByReviewId(reviewId);
        reviewCommentRepository.deleteByReviewId(reviewId);
        reviewRepository.delete(review);
    }
}
