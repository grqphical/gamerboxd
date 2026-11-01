package com.gamerboxd.gamerboxd.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.repositories.ReviewRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);
    private final User olivia = new User(2, "olivia", "olivia@gmail.com", "pw", UserRole.USER);

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void create_savesTrimmedReview() {
        when(userRepository.getReferenceById(1)).thenReturn(john);
        
        reviewService.create(1, "Hades", 5, "Great game");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertSame(john, saved.getUser());
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
        when(userRepository.getReferenceById(1)).thenReturn(john);
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
            new Review(john, "Hades", 5, "Great"),
            new Review(olivia, "Celeste", 4, "Evil but fun"),
            new Review(john, "Hollow Knight", 5, "Amazing"));
        when(reviewRepository.findAllByOrderByCreatedAtDesc()).thenReturn(reviews);

        List<Review> result = reviewService.getAll();

        assertEquals(3, result.size());
        assertEquals(reviews, result);
    }
}
