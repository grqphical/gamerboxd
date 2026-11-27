package com.gamerboxd.gamerboxd.controllers;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.services.GameService;

@Controller 
public class SearchController {
    private final GameService gameService;

    public SearchController(GameService gameService) {
        this.gameService = gameService;
    }   

    @GetMapping("/search")
    public String search(@AuthenticationPrincipal AuthenticatedUser currentUser,
                        @RequestParam(defaultValue = "") String q, Model model) {
        String query = q.trim();

        final int searchLimit = 21; // search page shows a maximum of 7x3 games
        // an empty search would still spend an IGDB request
        List<Game> results = query.isEmpty() 
                           ? List.of() : gameService.getSearchResultsByName(query, searchLimit);
        List<Game> sortedResults = gameService.sortByRating(results);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("query", query);
        model.addAttribute("sortedResults", sortedResults);
        return "search";
    }
}
