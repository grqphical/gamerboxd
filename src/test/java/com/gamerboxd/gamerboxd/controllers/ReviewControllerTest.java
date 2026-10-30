package com.gamerboxd.gamerboxd.controllers;

import static org.mockito.ArgumentMatchers.anyInt;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.services.ReviewService;

class ReviewControllerTest {
    private ReviewService reviewService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reviewService = mock(ReviewService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewService)).build();
    }

    @Test
    void list_showsReviews() throws Exception {
        List<Review> reviews = List.of(new Review(1, "Hades", 5, "Great"));
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
                .param("userId", "1")
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
                .when(reviewService).create(anyInt(), anyString(), eq(9), anyString());

        mockMvc.perform(post("/reviews")
                .param("userId", "1")
                .param("gameName", "Hades")
                .param("rating", "9")
                .param("description", "Great"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/form"))
                .andExpect(model().attribute("error", "Rating must be between 1 and 5."));
    }
}
