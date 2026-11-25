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
import com.gamerboxd.gamerboxd.services.NotificationService;

@Controller
public class SearchController {
    private final GameService gameService;
    private final NotificationService notificationService;

    public SearchController(GameService gameService, NotificationService notificationService) {
        this.gameService = gameService;
        this.notificationService = notificationService;
    }

    @GetMapping("/search")
    public String search(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "") String q, Model model) {
        String query = q.trim();
        // an empty search would still spend an IGDB request
        List<Game> results = query.isEmpty() ? List.of() : gameService.getSearchResultsByName(query, 20);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("query", query);
        model.addAttribute("results", results);
        if (currentUser != null)
            model.addAttribute("notificationCount", notificationService.getUserNotificationCount(currentUser.getId()));
        return "search";
    }
}
