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
        model.addAttribute("reviews", reviewService.getAll());
        model.addAttribute("currentUser", currentUser);
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
