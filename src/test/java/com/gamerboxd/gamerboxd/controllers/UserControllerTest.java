package com.gamerboxd.gamerboxd.controllers;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Optional;
import java.util.List;
import java.util.Map;
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
import com.gamerboxd.gamerboxd.models.GameStatus;
import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserPrivacy;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.ReviewCommentService;
import com.gamerboxd.gamerboxd.services.ReviewLikeService;
import com.gamerboxd.gamerboxd.services.ReviewService;
import com.gamerboxd.gamerboxd.services.UserService;

class UserControllerTest {
    private UserService userService;
    private GameService gameService;
    private ReviewService reviewService;
    private ReviewLikeService reviewLikeService;
    private ReviewCommentService reviewCommentService;
    private MockMvc mockMvc;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);
    private final User olivia = new User(2, "olivia", "olivia@gmail.com", "pw", UserRole.USER);

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        reviewService = mock(ReviewService.class);
        gameService = mock(GameService.class);
        reviewLikeService = mock(ReviewLikeService.class);
        reviewCommentService = mock(ReviewCommentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new UserController(userService, reviewService, gameService, reviewLikeService, reviewCommentService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        when(userService.getUserById(1)).thenReturn(Optional.of(john));
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
    void profile_anonymous_showsPublicProfile() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("user", john))
                .andExpect(model().attribute("isOwnProfile", false));
    }

    @Test
    void profile_anonymous_redirectsPrivateProfileToLogin() throws Exception {
        john.setPrivacy(UserPrivacy.PRIVATE);

        mockMvc.perform(get("/users/1"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void profile_signedIn_showsOwnProfile() throws Exception {
        signInAs(john);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isOwnProfile", true));
    }

    @Test
    void profile_signedIn_showsOtherUsersProfile() throws Exception {
        signInAs(olivia);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isOwnProfile", false));
    }

    @Test
    void profile_includesLikeCountsCommentCountsAndLikedIds() throws Exception {
        signInAs(john);
        Review review = new Review(olivia, 26226, 4, "Fun");
        review.setId(11);
        when(userService.getUserById(2)).thenReturn(Optional.of(olivia));
        when(reviewService.getAllByUserId(2)).thenReturn(List.of(review));
        when(reviewLikeService.getLikeCounts(List.of(11))).thenReturn(Map.of(11, 4L));
        when(reviewCommentService.getCommentCounts(List.of(11))).thenReturn(Map.of(11, 2L));
        when(reviewLikeService.getLikedReviewIds(List.of(11), 1)).thenReturn(Set.of(11));

        mockMvc.perform(get("/users/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("likeCounts", Map.of(11, 4L)))
                .andExpect(model().attribute("commentCounts", Map.of(11, 2L)))
                .andExpect(model().attribute("likedIds", Set.of(11)));
    }

    @Test
    void profile_anonymous_hasNoLikedIds() throws Exception {
        Review review = new Review(john, 113112, 5, "Great");
        review.setId(10);
        when(reviewService.getAllByUserId(1)).thenReturn(List.of(review));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("likedIds", Set.of()));

        verify(reviewLikeService, never()).getLikedReviewIds(anyCollection(), anyInt());
    }

    @Test
    void updateGameStatus_savesStatusForOwnProfile() throws Exception {
        signInAs(john);
        when(gameService.getGameByID(99)).thenReturn(Optional.of(mock(Game.class)));

        mockMvc.perform(post("/users/1/games/status")
                        .param("igdbID", "99")
                        .param("status", "PLAYED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/1"));

        org.junit.jupiter.api.Assertions.assertEquals(GameStatus.PLAYED, john.getGameStatuses().get(99));
        verify(userService).save(john);
    }

    @Test
    void updateGameStatus_rejectsOtherUsersProfile() throws Exception {
        signInAs(olivia);

        mockMvc.perform(post("/users/1/games/status")
                        .param("igdbID", "99")
                        .param("status", "PLAYED"))
                .andExpect(status().isForbidden());

        verify(userService, never()).save(john);
    }
}
