package com.gamerboxd.gamerboxd.services;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.ReviewLike;
import com.gamerboxd.gamerboxd.repositories.ReviewLikeRepository;
import com.gamerboxd.gamerboxd.repositories.ReviewRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@Service 
public class ReviewLikeService {
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewLikeService(
            ReviewLikeRepository reviewLikeRepository,
            ReviewRepository reviewRepository,
            UserRepository userRepository) {
        this.reviewLikeRepository = reviewLikeRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    /* Likes the review if not liked yet, otherwise removes the like. Returns true if now liked. */
    public boolean toggleLike(int reviewId, int userId) {
        Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found."));

        Optional<ReviewLike> existing = reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId);
        if (existing.isPresent()) { // like exists
            reviewLikeRepository.delete(existing.get());
            return false;
        }
        reviewLikeRepository.save(new ReviewLike(review, userRepository.getReferenceById(userId)));
        return true;
    }

    public long getLikeCount(int reviewId) {
        return reviewLikeRepository.countByReviewId(reviewId);
    }
}
