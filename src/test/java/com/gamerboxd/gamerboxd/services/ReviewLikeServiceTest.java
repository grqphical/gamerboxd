package com.gamerboxd.gamerboxd.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.ReviewLike;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.repositories.ReviewLikeRepository;
import com.gamerboxd.gamerboxd.repositories.ReviewRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReviewLikeServiceTest {
    @Mock
    private ReviewLikeRepository reviewLikeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewLikeService reviewLikeService;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);
    private final User olivia = new User(2, "olivia", "olivia@gmail.com", "pw", UserRole.USER);
    private final Review johnsReview = new Review(john, 113112, 5, "Great");

    @Test
    void toggleLike_notLiked_createsLikeAndReturnsTrue() {
        when(reviewRepository.findById(5)).thenReturn(Optional.of(johnsReview));
        when(reviewLikeRepository.findByReviewIdAndUserId(5, 2)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(2)).thenReturn(olivia);

        assertTrue(reviewLikeService.toggleLike(5, 2));

        ArgumentCaptor<ReviewLike> captor = ArgumentCaptor.forClass(ReviewLike.class);
        verify(reviewLikeRepository).save(captor.capture());
        assertSame(johnsReview, captor.getValue().getReview());
        assertSame(olivia, captor.getValue().getUser());
    }

    @Test
    void toggleLike_alreadyLiked_removesLikeAndReturnsFalse() {
        ReviewLike existing = new ReviewLike(johnsReview, olivia);
        when(reviewRepository.findById(5)).thenReturn(Optional.of(johnsReview));
        when(reviewLikeRepository.findByReviewIdAndUserId(5, 2)).thenReturn(Optional.of(existing));

        assertFalse(reviewLikeService.toggleLike(5, 2));

        verify(reviewLikeRepository).delete(existing);
        verify(reviewLikeRepository, never()).save(any());
    }

    @Test
    void toggleLike_ownReview_isAllowed() {
        when(reviewRepository.findById(5)).thenReturn(Optional.of(johnsReview));
        when(reviewLikeRepository.findByReviewIdAndUserId(5, 1)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1)).thenReturn(john);

        assertTrue(reviewLikeService.toggleLike(5, 1));

        verify(reviewLikeRepository).save(any(ReviewLike.class));
    }

    @Test
    void toggleLike_missingReview_throwsNotFound() {
        when(reviewRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewLikeService.toggleLike(99, 2));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(reviewLikeRepository, never()).save(any());
    }

    @Test
    void getLikeCount_returnsRepositoryCount() {
        when(reviewLikeRepository.countByReviewId(5)).thenReturn(3L);

        assertEquals(3L, reviewLikeService.getLikeCount(5));
    }

    @Test
    void getLikeCounts_fillsZeroForUnlikedReviews() {
        when(reviewLikeRepository.countByReviewIds(List.of(1, 2)))
                .thenReturn(List.<Object[]>of(new Object[] { 1, 3L }));

        assertEquals(Map.of(1, 3L, 2, 0L), reviewLikeService.getLikeCounts(List.of(1, 2)));
    }

    @Test
    void getLikeCounts_noReviews_skipsQuery() {
        assertEquals(Map.of(), reviewLikeService.getLikeCounts(List.of()));
        verify(reviewLikeRepository, never()).countByReviewIds(any());
    }

    @Test
    void getLikedReviewIds_returnsOnlyLikedOnes() {
        when(reviewLikeRepository.findLikedReviewIds(List.of(1, 2), 2)).thenReturn(Set.of(1));

        assertEquals(Set.of(1), reviewLikeService.getLikedReviewIds(List.of(1, 2), 2));
    }

    @Test
    void getLikedReviewIds_noReviews_skipsQuery() {
        assertEquals(Set.of(), reviewLikeService.getLikedReviewIds(List.of(), 2));
        verify(reviewLikeRepository, never()).findLikedReviewIds(any(), anyInt());
    }
}
