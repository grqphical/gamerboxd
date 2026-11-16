package com.gamerboxd.gamerboxd.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.ReviewComment;
import com.gamerboxd.gamerboxd.repositories.ReviewCommentRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@Service
public class ReviewCommentService {
    private static final int MAX_LENGTH = 1000;

    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public ReviewCommentService(ReviewCommentRepository reviewCommentRepository,
                                ReviewService reviewService,
                                UserRepository userRepository) {
        this.reviewCommentRepository = reviewCommentRepository;
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    public List<ReviewComment> getComments(int reviewId) {
        return reviewCommentRepository.findByReviewIdOrderByCreatedAtAsc(reviewId);
    }

    public ReviewComment add(int reviewId, int userId, String content) {
        Review review = reviewService.getVisibleReview(reviewId, userId);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment can't be empty.");
        }
        if (content.trim().length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Comments are limited to " + MAX_LENGTH + " characters.");
        }
        return reviewCommentRepository.save(
                new ReviewComment(review, userRepository.getReferenceById(userId), content.trim()));
    }

    public void delete(int commentId, int userId) {
        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found."));
        if (comment.getUser().getId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own comments.");
        }
        reviewCommentRepository.delete(comment);
    }
}
