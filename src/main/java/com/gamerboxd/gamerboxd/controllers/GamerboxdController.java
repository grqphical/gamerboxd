package com.gamerboxd.gamerboxd.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.services.ReviewService;

@Controller
public class GamerboxdController {
    private final ReviewService reviewService;

    public GamerboxdController(ReviewService reviewService) {
        this.reviewService = reviewService;
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
}
