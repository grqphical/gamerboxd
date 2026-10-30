package com.gamerboxd.gamerboxd.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.repositories.ReviewRepository;
import com.gamerboxd.gamerboxd.services.ReviewService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@Controller 
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/reviews")
    public String List(Model model) {
        model.addAttribute("reviews", reviewService.getAll());
        return "reviews/list";
    }

    @GetMapping("/reviews/new")
    public String form() {
        return "reviews/form";
    }

    @PostMapping("/reviews")
    public String create(@RequestParam int userId,
                         @RequestParam String gameName,
                         @RequestParam int rating,
                         @RequestParam String description,
                         Model model) {
        try {
            reviewService.create(userId, gameName, rating, description);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "reviews/form";
        }
        return "redirect:/reviews";
    }
}
