package com.gamerboxd.gamerboxd.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.ReviewService;

class ReviewControllerTest {
    private ReviewService reviewService;
    private MockMvc mockMvc;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);
    private final User olivia = new User(2, "olivia", "olivia@gmail.com", "pw", UserRole.USER);

    @BeforeEach
    void setUp() {
        reviewService = mock(ReviewService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewService))
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
    void list_showsReviews() throws Exception {
        List<Review> reviews = List.of(
        new Review(john, "Hades", 5, "Great"),
        new Review(olivia, "Celeste", 4, "Tough but fun"));
        Mockito.when(reviewService.getAll()).thenReturn(reviews);

        mockMvc.perform(get("/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/list"))
                .andExpect(model().attribute("reviews", reviews));
    }

    @Test
    void form_showsForm() throws Exception {
        mockMvc.perform(get("/reviews/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/form"));
    }

    @Test
    void create_validReview_redirectsToList() throws Exception {
        mockMvc.perform(post("/reviews")
                .param("gameName", "Hades")
                .param("rating", "5")
                .param("description", "Great"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews"));

        verify(reviewService).create(1, "Hades", 5, "Great");
    }

    @Test
    void create_invalidReview_showsFormWithError() throws Exception {
        doThrow(new IllegalArgumentException("Rating must be between 1 and 5."))
                .when(reviewService).create(eq(1), anyString(), eq(9), anyString());

        mockMvc.perform(post("/reviews")
                .param("gameName", "Hades")
                .param("rating", "9")
                .param("description", "Great"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/form"))
                .andExpect(model().attribute("error", "Rating must be between 1 and 5."));
    }

    @Test
    void editForm_owner_showsForm() throws Exception {
        Review review = new Review(john, "Hades", 3, "Ok");
        Mockito.when(reviewService.getOwnedReview(5, 1)).thenReturn(review);

        mockMvc.perform(get("/reviews/5/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/edit"))
                .andExpect(model().attribute("review", review));
    }

    @Test
    void editForm_notOwner_returnsForbidden() throws Exception {
        Mockito.when(reviewService.getOwnedReview(5, 1))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/reviews/5/edit"))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_valid_redirectsToList() throws Exception {
        mockMvc.perform(post("/reviews/5")
                .param("rating", "4")
                .param("description", "Updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews"));

        verify(reviewService).update(5, 1, 4, "Updated");
    }

    @Test
    void update_invalid_showsEditFormWithError() throws Exception {
        Review review = new Review(john, "Hades", 3, "Ok");
        doThrow(new IllegalArgumentException("Rating must be between 1 and 5."))
                .when(reviewService).update(eq(5), eq(1), eq(9), anyString());
        Mockito.when(reviewService.getOwnedReview(5, 1)).thenReturn(review);

        mockMvc.perform(post("/reviews/5")
                .param("rating", "9")
                .param("description", "Updated"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/edit"))
                .andExpect(model().attribute("error", "Rating must be between 1 and 5."))
                .andExpect(model().attribute("review", review));
    }

    @Test
    void update_notOwner_returnsForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(reviewService).update(eq(5), eq(1), eq(4), anyString());

        mockMvc.perform(post("/reviews/5")
                .param("rating", "4")
                .param("description", "Updated"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_owner_redirectsToList() throws Exception {
        mockMvc.perform(post("/reviews/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews"));

        verify(reviewService).delete(5, 1);
    }

    @Test
    void delete_fromProfile_redirectsToProfile() throws Exception {
        mockMvc.perform(post("/reviews/5/delete").param("from", "profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/1"));

        verify(reviewService).delete(5, 1);
    }

    @Test
    void delete_notOwner_returnsForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(reviewService).delete(5, 1);

        mockMvc.perform(post("/reviews/5/delete"))
                .andExpect(status().isForbidden());
    }

    @Test
    void editForm_fromProfile_passesFromToView() throws Exception {
        Review review = new Review(john, "Hades", 3, "Ok");
        Mockito.when(reviewService.getOwnedReview(5, 1)).thenReturn(review);

        mockMvc.perform(get("/reviews/5/edit").param("from", "profile"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("from", "profile"));
    }

    @Test
    void update_fromProfile_redirectsToProfile() throws Exception {
        mockMvc.perform(post("/reviews/5")
                .param("from", "profile")
                .param("rating", "4")
                .param("description", "Updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/1"));

        verify(reviewService).update(5, 1, 4, "Updated");
    }

    @Test
    void update_invalidFromProfile_keepsFromInModel() throws Exception {
        Review review = new Review(john, "Hades", 3, "Ok");
        doThrow(new IllegalArgumentException("Rating must be between 1 and 5."))
                .when(reviewService).update(eq(5), eq(1), eq(9), anyString());
        Mockito.when(reviewService.getOwnedReview(5, 1)).thenReturn(review);

        mockMvc.perform(post("/reviews/5")
                .param("from", "profile")
                .param("rating", "9")
                .param("description", "Updated"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/edit"))
                .andExpect(model().attribute("from", "profile"));
    }
}
