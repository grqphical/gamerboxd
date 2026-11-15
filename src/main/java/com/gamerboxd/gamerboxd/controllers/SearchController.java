package com.gamerboxd.gamerboxd.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.services.GameService;

@Controller 
public class SearchController {
    private final GameService gameService;

    public SearchController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q, Model model) {
        String query = q.trim();
        List<Game> results = gameService.getSearchResultsByName(query, 20);

        model.addAttribute("query", query);
        model.addAttribute("results", results);
        return "search";
    }
}
