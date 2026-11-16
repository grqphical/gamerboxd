package com.gamerboxd.gamerboxd.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.gamerboxd.gamerboxd.repositories.ReviewCommentRepository;
import com.gamerboxd.gamerboxd.repositories.ReviewLikeRepository;
import com.gamerboxd.gamerboxd.repositories.ReviewRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

// Deleting a user has to clear everything that references them, so run it against a real database
@SpringBootTest
@ActiveProfiles("test")
class UserDeletionTest {
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewLikeRepository reviewLikeRepository;
    @Autowired
    private ReviewCommentRepository reviewCommentRepository;

    private User john;
    private User olivia;
    private Review johnsReview;
    private Review oliviasReview;

    @BeforeEach
    void setUp() {
        clear();
        john = userRepository.save(new User(0, "john", "john@example.com", "pw", UserRole.USER));
        olivia = userRepository.save(new User(0, "olivia", "olivia@example.com", "pw", UserRole.USER));
        johnsReview = reviewRepository.save(new Review(john, 113112, 5, "Great"));
        oliviasReview = reviewRepository.save(new Review(olivia, 26226, 2, "Meh"));
    }

    @AfterEach
    void clear() {
        reviewCommentRepository.deleteAll();
        reviewLikeRepository.deleteAll();
        reviewRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void deleteUser_withNoActivity_removesUser() {
        userService.deleteUser(olivia.getId());

        assertFalse(userRepository.existsById(olivia.getId()));
        assertTrue(userRepository.existsById(john.getId()));
    }

    @Test
    void deleteUser_removesOtherUsersLikesAndCommentsOnTheirReviews() {
        reviewLikeRepository.save(new ReviewLike(johnsReview, olivia));
        reviewCommentRepository.save(new ReviewComment(johnsReview, olivia, "Agreed"));

        userService.deleteUser(john.getId());

        assertFalse(userRepository.existsById(john.getId()));
        assertFalse(reviewRepository.existsById(johnsReview.getId()));
        assertEquals(0, reviewLikeRepository.count());
        assertEquals(0, reviewCommentRepository.count());
    }

    @Test
    void deleteUser_removesTheirLikesAndCommentsOnOtherReviews() {
        reviewLikeRepository.save(new ReviewLike(oliviasReview, john));
        reviewCommentRepository.save(new ReviewComment(oliviasReview, john, "Disagree"));
        reviewLikeRepository.save(new ReviewLike(oliviasReview, olivia));

        userService.deleteUser(john.getId());

        assertFalse(userRepository.existsById(john.getId()));
        assertTrue(reviewRepository.existsById(oliviasReview.getId()));
        assertEquals(1, reviewLikeRepository.count());
        assertEquals(0, reviewCommentRepository.count());
    }
}
