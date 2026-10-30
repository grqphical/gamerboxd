package com.gamerboxd.gamerboxd.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.repositories.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void create_savesTrimmedReview() {
        reviewService.create(1, "  Hades  ", 5, "  Great game  ");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertEquals(1, saved.getUserId());
        assertEquals("Hades", saved.getGameName());
        assertEquals(5, saved.getRating());
        assertEquals("Great game", saved.getDescription());
    }

    @Test
    void create_blankGameName_throws() {
        assertThrows(IllegalArgumentException.class, () -> reviewService.create(1, "  ", 4, "Good"));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void create_nullGameName_throws() {
        assertThrows(IllegalArgumentException.class, () -> reviewService.create(1, null, 4, "Good"));
        verify(reviewRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 6, 100 })
    void create_ratingOutOfRange_throws(int rating) {
        assertThrows(IllegalArgumentException.class, () -> reviewService.create(1, "Hades", rating, "Good"));
        verify(reviewRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5 })
    void create_ratingInRange_saves(int rating) {
        reviewService.create(1, "Hades", rating, "Good");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void create_blankDescription_throws() {
        assertThrows(IllegalArgumentException.class, () -> reviewService.create(1, "Hades", 4, "   "));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void getAll_returnsRepositoryResults() {
        List<Review> reviews = List.of(
            new Review(1, "Hades", 5, "Great"),
            new Review(2, "Celeste", 4, "Evil but fun"),
            new Review(1, "Hollow Knight", 5, "Amazing"));
        when(reviewRepository.findAllByOrderByCreatedAtDesc()).thenReturn(reviews);

        List<Review> result = reviewService.getAll();

        assertEquals(3, result.size());
        assertEquals(reviews, result);
    }
}
