package com.gamerboxd.gamerboxd.controllers;

import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.services.ReviewService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.models.Game;


@Controller
public class GamerboxdController {
    private final ReviewService reviewService;
    private final GameService gameService;

    public GamerboxdController(ReviewService reviewService, GameService gameService) {
        this.reviewService = reviewService;
        this.gameService = gameService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        // dont show private reviews on the index
        // temporary solution until later iterations which will need to consider following/followers
        model.addAttribute("reviews", reviewService.getAll().stream()
                .filter(r -> !r.getUser().isPrivateAccount())
                .toList());

        model.addAttribute("currentUser", currentUser);
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**Purely for manual game database/cache testing*/
    @GetMapping("/IGDBtest/{id}")
    public String IGDBtest(@PathVariable int id) {
        Optional<Game> game = gameService.getGameByID(id);
        if (game.isPresent() == false) {
            System.out.println("ID does not match an existing IGDB game");
        }
        return "redirect:/";
    }
    
}
