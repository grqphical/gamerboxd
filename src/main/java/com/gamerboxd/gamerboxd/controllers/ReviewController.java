package com.gamerboxd.gamerboxd.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.services.ReviewService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;



@Controller 
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/reviews")
    public String List(@AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        model.addAttribute("reviews", reviewService.getAll());
        model.addAttribute("currentUser", currentUser); // null when logged out
        return "reviews/list";
    }

    @PostMapping("/reviews/{id}/delete")
    public String delete(@PathVariable int id,
                        @AuthenticationPrincipal AuthenticatedUser currentUser) {
        reviewService.delete(id, currentUser.getId());
        return "redirect:/reviews";
    }

    @GetMapping("/reviews/{id}/edit")
    public String editForm(@PathVariable int id,
                        @AuthenticationPrincipal AuthenticatedUser currentUser,
                        Model model) {
        model.addAttribute("review", reviewService.getOwnedReview(id, currentUser.getId()));
        return "reviews/edit";
    }

    @PostMapping("/reviews/{id}")
    public String update(@PathVariable int id,
                        @AuthenticationPrincipal AuthenticatedUser currentUser,
                        @RequestParam int rating,
                        @RequestParam String description,
                        Model model) {
        try {
            reviewService.update(id, currentUser.getId(), rating, description);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("review", reviewService.getOwnedReview(id, currentUser.getId()));
            return "reviews/edit";
        }
        return "redirect:/reviews";
    }

    @GetMapping("/reviews/new")
    public String form() {
        return "reviews/form";
    }

    @PostMapping("/reviews")
    public String create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                         @RequestParam String gameName,
                         @RequestParam int rating,
                         @RequestParam String description,
                         Model model) {
        try {
            reviewService.create(currentUser.getId(), gameName, rating, description);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "reviews/form";
        }
        return "redirect:/reviews";
    }
}
