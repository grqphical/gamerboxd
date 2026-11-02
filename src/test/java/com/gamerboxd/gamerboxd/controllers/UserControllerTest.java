package com.gamerboxd.gamerboxd.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserPrivacy;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.ReviewService;
import com.gamerboxd.gamerboxd.services.UserService;

class UserControllerTest {
    private UserService userService;
    private MockMvc mockMvc;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);
    private final User olivia = new User(2, "olivia", "olivia@gmail.com", "pw", UserRole.USER);

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, mock(ReviewService.class)))
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
}
