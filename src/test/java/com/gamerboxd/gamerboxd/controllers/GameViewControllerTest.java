package com.gamerboxd.gamerboxd.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserPrivacy;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.ReviewLikeService;
import com.gamerboxd.gamerboxd.services.ReviewService;

class GameViewControllerTest {
    private static final int GAME_ID = 113112;

    private GameService gameService;
    private ReviewService reviewService;
    private ReviewLikeService reviewLikeService;
    private MockMvc mockMvc;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);
    private final User olivia = new User(2, "olivia", "olivia@gmail.com", "pw", UserRole.USER);
    private final Game game = mock(Game.class);
    private Review publicReview;
    private Review privateReview;

    @BeforeEach
    void setUp() {
        gameService = mock(GameService.class);
        reviewService = mock(ReviewService.class);
        reviewLikeService = mock(ReviewLikeService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new GameViewController(gameService, reviewService, reviewLikeService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        olivia.setPrivacy(UserPrivacy.PRIVATE);
        publicReview = new Review(john, GAME_ID, 5, "Great");
        publicReview.setId(10);
        privateReview = new Review(olivia, GAME_ID, 2, "Meh");
        privateReview.setId(11);

        when(gameService.getGameByID(GAME_ID)).thenReturn(Optional.of(game));
        when(reviewService.getAllByGameId(GAME_ID)).thenReturn(List.of(publicReview, privateReview));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void signInAs(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void gameView_hidesPrivateAccountsReviewsFromOthers() throws Exception {
        signInAs(john);
        when(reviewLikeService.getLikeCounts(List.of(10))).thenReturn(Map.of(10, 2L));
        when(reviewLikeService.getLikedReviewIds(List.of(10), 1)).thenReturn(Set.of(10));

        mockMvc.perform(get("/games/" + GAME_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("games"))
                .andExpect(model().attribute("game", game))
                .andExpect(model().attribute("reviews", List.of(publicReview)))
                .andExpect(model().attribute("likeCounts", Map.of(10, 2L)))
                .andExpect(model().attribute("likedIds", Set.of(10)));
    }

    @Test
    void gameView_showsPrivateAuthorTheirOwnReview() throws Exception {
        signInAs(olivia);

        mockMvc.perform(get("/games/" + GAME_ID))
                .andExpect(model().attribute("reviews", List.of(publicReview, privateReview)));
    }

    @Test
    void gameView_anonymous_seesOnlyPublicReviewsAndNoLikedIds() throws Exception {
        mockMvc.perform(get("/games/" + GAME_ID))
                .andExpect(model().attribute("reviews", List.of(publicReview)))
                .andExpect(model().attribute("likedIds", Set.of()));
    }

    @Test
    void gameView_unknownGame_isNotFound() throws Exception {
        when(gameService.getGameByID(404)).thenReturn(Optional.empty());

        mockMvc.perform(get("/games/404"))
                .andExpect(status().isNotFound());
    }
}
