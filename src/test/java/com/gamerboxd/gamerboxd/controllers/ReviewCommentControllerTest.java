package com.gamerboxd.gamerboxd.controllers;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.ReviewComment;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.NotificationService;
import com.gamerboxd.gamerboxd.services.ReviewCommentService;
import com.gamerboxd.gamerboxd.services.ReviewService;

class ReviewCommentControllerTest {
    private ReviewService reviewService;
    private ReviewCommentService reviewCommentService;
    private GameService gameService;
    private NotificationService notificationService;
    private MockMvc mockMvc;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);
    private final User olivia = new User(2, "olivia", "olivia@gmail.com", "pw", UserRole.USER);
    private final Review johnsReview = new Review(john, 5, 5, "Great");

    @BeforeEach
    void setUp() {
        reviewService = mock(ReviewService.class);
        reviewCommentService = mock(ReviewCommentService.class);
        gameService = mock(GameService.class);
        notificationService = mock(NotificationService.class);
        when(gameService.getGameNames(List.of(5))).thenReturn(Map.of(5, "Hades"));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ReviewCommentController(reviewService, reviewCommentService, gameService, notificationService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        AuthenticatedUser principal = new AuthenticatedUser(john);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reviewPage_showsReviewAndComments() throws Exception {
        List<ReviewComment> comments = List.of(new ReviewComment(johnsReview, olivia, "Nice"));
        when(reviewService.getVisibleReview(5, 1)).thenReturn(johnsReview);
        when(reviewCommentService.getComments(5)).thenReturn(comments);

        mockMvc.perform(get("/reviews/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/detail"))
                .andExpect(model().attribute("review", johnsReview))
                .andExpect(model().attribute("comments", comments));
    }

    @Test
    void reviewPage_hiddenReview_returnsNotFound() throws Exception {
        when(reviewService.getVisibleReview(5, 1))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/reviews/5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addComment_valid_redirectsToReviewPage() throws Exception {
        mockMvc.perform(post("/reviews/5/comments").param("content", "Nice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews/5"));

        verify(reviewCommentService).add(5, 1, "Nice");
    }

    @Test
    void addComment_invalid_showsPageWithError() throws Exception {
        doThrow(new IllegalArgumentException("Comment can't be empty."))
                .when(reviewCommentService).add(5, 1, " ");
        when(reviewService.getVisibleReview(5, 1)).thenReturn(johnsReview);

        mockMvc.perform(post("/reviews/5/comments").param("content", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/detail"))
                .andExpect(model().attribute("error", "Comment can't be empty."));
    }

    @Test
    void deleteComment_redirectsBackToReview() throws Exception {
        mockMvc.perform(post("/comments/7/delete").param("reviewId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews/5"));

        verify(reviewCommentService).delete(7, 1);
    }

    @Test
    void deleteComment_notOwner_returnsForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(reviewCommentService).delete(7, 1);

        mockMvc.perform(post("/comments/7/delete").param("reviewId", "5"))
                .andExpect(status().isForbidden());
    }
}