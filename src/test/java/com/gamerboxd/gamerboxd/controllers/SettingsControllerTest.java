package com.gamerboxd.gamerboxd.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.NotificationService;
import com.gamerboxd.gamerboxd.services.ReviewService;
import com.gamerboxd.gamerboxd.services.UserService;

class SettingsControllerTest {
    private UserService userService;
    private NotificationService notificationService;
    private ReviewService reviewService;
    private MockMvc mockMvc;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);
    private final User olivia = new User(2, "olivia", "olivia@gmail.com", "pw", UserRole.USER);

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        notificationService = mock(NotificationService.class);
        reviewService = mock(ReviewService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SettingsController(userService, reviewService, notificationService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        when(userService.getUserById(1)).thenReturn(Optional.of(john));
        when(userService.getUserById(2)).thenReturn(Optional.of(olivia));
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

    private MockHttpServletRequestBuilder settingsPost(int id) {
        return post("/users/" + id + "/settings")
                .param("username", "johnny")
                .param("email", "johnny@gmail.com")
                .param("password", "")
                .param("privacy", "private")
                .param("description", "Hi")
                .param("pronouns", "he/him");
    }

    @Test
    void settingsPage_owner_showsSettings() throws Exception {
        signInAs(john);

        mockMvc.perform(get("/users/1/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/settings"))
                .andExpect(model().attribute("user", john));
    }

    @Test
    void settingsPage_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/users/1/settings"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void settingsPage_otherUser_redirectsToTheirProfile() throws Exception {
        signInAs(olivia);

        mockMvc.perform(get("/users/1/settings"))
                .andExpect(redirectedUrl("/users/1"));
    }

    @Test
    void settingsPage_unknownUser_isNotFound() throws Exception {
        signInAs(john);

        mockMvc.perform(get("/users/99/settings"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_owner_savesAndRedirectsToProfile() throws Exception {
        signInAs(john);

        mockMvc.perform(settingsPost(1))
                .andExpect(redirectedUrl("/users/1"));

        verify(userService).updateSettings(john, "johnny", "johnny@gmail.com", "", "private", "Hi", "he/him");
    }

    @Test
    void update_otherUser_changesNothing() throws Exception {
        signInAs(olivia);

        mockMvc.perform(settingsPost(1))
                .andExpect(redirectedUrl("/users/1"));

        verify(userService, never()).updateSettings(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void update_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(settingsPost(1))
                .andExpect(redirectedUrl("/login"));

        verify(userService, never()).updateSettings(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void update_invalid_showsFormWithErrorAndStaysSignedIn() throws Exception {
        signInAs(john);
        doThrow(new IllegalArgumentException("- Enter a valid email address.\n"))
                .when(userService).updateSettings(any(), any(), any(), any(), any(), any(), any());

        mockMvc.perform(settingsPost(1))
                .andExpect(status().isOk())
                .andExpect(view().name("users/settings"))
                .andExpect(model().attribute("error", "- Enter a valid email address.\n"))
                .andExpect(model().attribute("user", john))
                .andExpect(model().attributeExists("currentUser"));
    }
}
