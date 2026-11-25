package com.gamerboxd.gamerboxd.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.SteamGame;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.SteamLibraryLinkJobService;
import com.gamerboxd.gamerboxd.services.SteamService;
import com.gamerboxd.gamerboxd.services.NotificationService;
import com.gamerboxd.gamerboxd.services.UserService;

class SteamLoginControllerTest {
    private static final String STEAM_ID = "76561198000000000";

    private SteamService steamService;
    private UserService userService;
    private SteamLibraryLinkJobService linkJobService;
    private NotificationService notificationService;
    private MockMvc mockMvc;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);

    @BeforeEach
    void setUp() {
        steamService = mock(SteamService.class);
        userService = mock(UserService.class);
        linkJobService = mock(SteamLibraryLinkJobService.class);
        notificationService = mock(NotificationService.class);

        SteamLoginController controller = new SteamLoginController(steamService, userService, linkJobService, notificationService);
        ReflectionTestUtils.setField(controller, "baseUrl", "http://localhost:8080/");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        when(userService.getUserById(1)).thenReturn(Optional.of(john));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void signIn() {
        AuthenticatedUser principal = new AuthenticatedUser(john);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void callback_cancelledLogin_isBadRequest() throws Exception {
        mockMvc.perform(get("/auth/steam/callback").param("openid.mode", "cancel"))
                .andExpect(status().isBadRequest());

        verify(steamService, never()).verifySteamLogin(any());
    }

    @Test
    void callback_returnToMismatch_isBadRequest() throws Exception {
        mockMvc.perform(get("/auth/steam/callback")
                .param("openid.mode", "id_res")
                .param("openid.return_to", "http://evil.example/auth/steam/callback"))
                .andExpect(status().isBadRequest());

        verify(steamService, never()).verifySteamLogin(any());
    }

    @Test
    void callback_verifiedLogin_redirectsToFinish() throws Exception {
        when(steamService.verifySteamLogin(any())).thenReturn(STEAM_ID);

        mockMvc.perform(get("/auth/steam/callback")
                .param("openid.mode", "id_res")
                .param("openid.return_to", "http://localhost:8080/auth/steam/callback"))
                .andExpect(redirectedUrl("http://localhost:8080/auth/steam/finish?steamid=" + STEAM_ID));
    }

    @Test
    void syncLibrary_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/auth/steam/sync-library"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void syncLibrary_noLinkedAccount_failsWithoutCallingSteam() throws Exception {
        signIn();

        mockMvc.perform(get("/auth/steam/sync-library"))
                .andExpect(status().isInternalServerError());

        verify(steamService, never()).getUserSteamLibrary(any());
    }

    @Test
    void syncLibrary_linkedAccount_showsLibrary() throws Exception {
        signIn();
        john.setSteamID(STEAM_ID);
        List<SteamGame> games = List.of(new SteamGame(620, "Portal 2", "icon", 300));
        when(steamService.getUserSteamLibrary(STEAM_ID)).thenReturn(games);

        mockMvc.perform(get("/auth/steam/sync-library"))
                .andExpect(status().isOk())
                .andExpect(view().name("steam/linkLibrary"))
                .andExpect(model().attribute("games", games));
    }

    @Test
    void linkLibrary_anonymous_redirectsToLoginWithoutImporting() throws Exception {
        mockMvc.perform(post("/auth/steam/link-library").param("selectedAppIds", "620"))
                .andExpect(redirectedUrl("/login"));

        verify(linkJobService, never()).linkUserSteamLibrary(anyInt(), anyList(), anyMap());
    }

    @Test
    void linkLibrary_signedIn_startsImport() throws Exception {
        signIn();

        mockMvc.perform(post("/auth/steam/link-library")
                .param("selectedAppIds", "620", "400")
                .param("gameNames[620]", "Portal 2"))
                .andExpect(redirectedUrl("/"));

        verify(linkJobService).linkUserSteamLibrary(eq(1), eq(List.of(620, 400)), anyMap());
    }

    @Test
    void linkLibrary_nothingSelected_skipsImport() throws Exception {
        signIn();

        mockMvc.perform(post("/auth/steam/link-library"))
                .andExpect(redirectedUrl("/"));

        verify(linkJobService, never()).linkUserSteamLibrary(anyInt(), anyList(), anyMap());
    }

    @Test
    void unlink_signedIn_unlinksAndReturnsToSettings() throws Exception {
        signIn();

        mockMvc.perform(get("/auth/steam/unlink"))
                .andExpect(redirectedUrl("/users/1/settings"));

        verify(userService).unlinkSteamAccount(1);
    }

    @Test
    void unlink_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/auth/steam/unlink"))
                .andExpect(redirectedUrl("/login"));

        verify(userService, never()).unlinkSteamAccount(anyInt());
    }
}
