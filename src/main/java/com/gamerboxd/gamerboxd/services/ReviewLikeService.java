package com.gamerboxd.gamerboxd.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
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

    /** Like count for every given review, including zeros, in a single query. */
    public Map<Integer, Long> getLikeCounts(Collection<Integer> reviewIds) {
        Map<Integer, Long> counts = new HashMap<>();
        if (reviewIds.isEmpty()) {
            return counts;
        }
        reviewIds.forEach(id -> counts.put(id, 0L));
        for (Object[] row : reviewLikeRepository.countByReviewIds(reviewIds)) {
            counts.put((Integer) row[0], (Long) row[1]);
        }
        return counts;
    }

    public Set<Integer> getLikedReviewIds(Collection<Integer> reviewIds, int userId) {
        if (reviewIds.isEmpty()) {
            return Set.of();
        }
        return reviewLikeRepository.findLikedReviewIds(reviewIds, userId);
    }
}
