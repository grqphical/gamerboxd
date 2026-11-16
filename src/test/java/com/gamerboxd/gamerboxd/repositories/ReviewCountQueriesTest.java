package com.gamerboxd.gamerboxd.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.ReviewComment;
import com.gamerboxd.gamerboxd.models.ReviewLike;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.ReviewCommentService;
import com.gamerboxd.gamerboxd.services.ReviewLikeService;

// Runs the batched count queries against a real database
@SpringBootTest
@ActiveProfiles("test")
class ReviewCountQueriesTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewLikeRepository reviewLikeRepository;
    @Autowired
    private ReviewCommentRepository reviewCommentRepository;
    @Autowired
    private ReviewLikeService reviewLikeService;
    @Autowired
    private ReviewCommentService reviewCommentService;

    private User john;
    private User olivia;
    private Review popular;
    private Review quiet;

    @BeforeEach
    void setUp() {
        clear();
        john = userRepository.save(new User(0, "john", "john@example.com", "pw", UserRole.USER));
        olivia = userRepository.save(new User(0, "olivia", "olivia@example.com", "pw", UserRole.USER));
        popular = reviewRepository.save(new Review(john, 113112, 5, "Great"));
        quiet = reviewRepository.save(new Review(olivia, 26226, 2, "Meh"));

        reviewLikeRepository.save(new ReviewLike(popular, john));
        reviewLikeRepository.save(new ReviewLike(popular, olivia));
        reviewCommentRepository.save(new ReviewComment(popular, olivia, "Agreed"));
    }

    @AfterEach
    void clear() {
        reviewCommentRepository.deleteAll();
        reviewLikeRepository.deleteAll();
        reviewRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void likeCounts_coverEveryRequestedReview() {
        assertEquals(Map.of(popular.getId(), 2L, quiet.getId(), 0L),
                reviewLikeService.getLikeCounts(List.of(popular.getId(), quiet.getId())));
    }

    @Test
    void likeCounts_ignoreReviewsNotRequested() {
        assertEquals(Map.of(quiet.getId(), 0L), reviewLikeService.getLikeCounts(List.of(quiet.getId())));
    }

    @Test
    void commentCounts_coverEveryRequestedReview() {
        assertEquals(Map.of(popular.getId(), 1L, quiet.getId(), 0L),
                reviewCommentService.getCommentCounts(List.of(popular.getId(), quiet.getId())));
    }

    @Test
    void likedReviewIds_areScopedToTheUser() {
        reviewLikeRepository.save(new ReviewLike(quiet, john));
        List<Integer> ids = List.of(popular.getId(), quiet.getId());

        assertEquals(Set.of(popular.getId(), quiet.getId()), reviewLikeService.getLikedReviewIds(ids, john.getId()));
        assertEquals(Set.of(popular.getId()), reviewLikeService.getLikedReviewIds(ids, olivia.getId()));
    }
}
