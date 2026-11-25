package com.gamerboxd.gamerboxd.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.NotificationService;

class SearchControllerTest {
    private GameService gameService;
    private NotificationService notificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        gameService = mock(GameService.class);
        notificationService = mock(NotificationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SearchController(gameService, notificationService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                // the "search" view shares its name with the /search URL, which the default resolver rejects
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
                .build();
    }

    @Test
    void search_trimsQueryAndShowsResults() throws Exception {
        List<Game> results = List.of(mock(Game.class));
        when(gameService.getSearchResultsByName("portal", 20)).thenReturn(results);

        mockMvc.perform(get("/search").param("q", "  portal "))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(model().attribute("query", "portal"))
                .andExpect(model().attribute("results", results));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void search_blankQuery_skipsLookup(String q) throws Exception {
        mockMvc.perform(get("/search").param("q", q))
                .andExpect(status().isOk())
                .andExpect(model().attribute("query", ""))
                .andExpect(model().attribute("results", List.of()));

        verifyNoInteractions(gameService);
    }

    @Test
    void search_missingQuery_skipsLookup() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("results", List.of()));

        verifyNoInteractions(gameService);
    }
}
