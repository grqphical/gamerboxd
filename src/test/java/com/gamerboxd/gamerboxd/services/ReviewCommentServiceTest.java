package com.gamerboxd.gamerboxd.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.ReviewComment;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.repositories.ReviewCommentRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReviewCommentServiceTest {
    @Mock
    private ReviewCommentRepository reviewCommentRepository;
    @Mock
    private ReviewService reviewService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewCommentService reviewCommentService;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);
    private final User olivia = new User(2, "olivia", "olivia@gmail.com", "pw", UserRole.USER);
    private final Review johnsReview = new Review(john, 5, 5, "Great");

    @Test
    void add_valid_savesTrimmedComment() {
        when(reviewService.getVisibleReview(5, 2)).thenReturn(johnsReview);
        when(userRepository.getReferenceById(2)).thenReturn(olivia);

        reviewCommentService.add(5, 2, "  Nice review  ");

        ArgumentCaptor<ReviewComment> captor = ArgumentCaptor.forClass(ReviewComment.class);
        verify(reviewCommentRepository).save(captor.capture());
        assertSame(johnsReview, captor.getValue().getReview());
        assertSame(olivia, captor.getValue().getUser());
        assertEquals("Nice review", captor.getValue().getContent());
    }

    @Test
    void add_blank_throws() {
        when(reviewService.getVisibleReview(5, 2)).thenReturn(johnsReview);

        assertThrows(IllegalArgumentException.class, () -> reviewCommentService.add(5, 2, "   "));
        verify(reviewCommentRepository, never()).save(any());
    }

    @Test
    void add_tooLong_throws() {
        when(reviewService.getVisibleReview(5, 2)).thenReturn(johnsReview);

        assertThrows(IllegalArgumentException.class,
                () -> reviewCommentService.add(5, 2, "a".repeat(1001)));
        verify(reviewCommentRepository, never()).save(any());
    }

    @Test
    void add_hiddenOrMissingReview_throwsNotFound() {
        when(reviewService.getVisibleReview(5, 2))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> reviewCommentService.add(5, 2, "Hi"));
        verify(reviewCommentRepository, never()).save(any());
    }

    @Test
    void getComments_returnsRepositoryResults() {
        List<ReviewComment> comments = List.of(
                new ReviewComment(johnsReview, olivia, "First"),
                new ReviewComment(johnsReview, john, "Second"));
        when(reviewCommentRepository.findByReviewIdOrderByCreatedAtAsc(5)).thenReturn(comments);

        assertEquals(comments, reviewCommentService.getComments(5));
    }

    @Test
    void delete_owner_deletesComment() {
        ReviewComment comment = new ReviewComment(johnsReview, olivia, "Hi");
        when(reviewCommentRepository.findById(7)).thenReturn(Optional.of(comment));

        reviewCommentService.delete(7, 2);

        verify(reviewCommentRepository).delete(comment);
    }

    @Test
    void delete_otherUser_throwsForbidden() {
        ReviewComment comment = new ReviewComment(johnsReview, olivia, "Hi");
        when(reviewCommentRepository.findById(7)).thenReturn(Optional.of(comment));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewCommentService.delete(7, 1));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(reviewCommentRepository, never()).delete(any(ReviewComment.class));
    }

    @Test
    void delete_missing_throwsNotFound() {
        when(reviewCommentRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewCommentService.delete(99, 1));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getCommentCounts_fillsZeroForUncommentedReviews() {
        when(reviewCommentRepository.countByReviewIds(List.of(1, 2)))
                .thenReturn(List.<Object[]>of(new Object[] { 2, 4L }));

        assertEquals(Map.of(1, 0L, 2, 4L), reviewCommentService.getCommentCounts(List.of(1, 2)));
    }

    @Test
    void getCommentCounts_noReviews_skipsQuery() {
        assertEquals(Map.of(), reviewCommentService.getCommentCounts(List.of()));
        verify(reviewCommentRepository, never()).countByReviewIds(any());
    }
}
