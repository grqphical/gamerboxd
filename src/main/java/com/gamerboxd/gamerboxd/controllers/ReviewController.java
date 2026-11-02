package com.gamerboxd.gamerboxd.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.services.ReviewService;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.services.UserService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Pages:
 * 
 * - /reviews/edit
 * 
 * - /reviews/form
 * 
 * - (removed) /reviews -> now just /
 */
@Controller
public class ReviewController {
    private final ReviewService reviewService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @PostMapping("/reviews/{id}/delete")
    public String delete(@PathVariable int id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String from) {
        reviewService.delete(id, currentUser.getId());
        return redirectAfter(from, currentUser);
    }

    @GetMapping("/reviews/{id}/edit")
    public String editForm(@PathVariable int id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String from,
            Model model) {
        model.addAttribute("review", reviewService.getOwnedReview(id, currentUser.getId()));
        model.addAttribute("from", from);
        model.addAttribute("currentUser", currentUser);
        return "reviews/edit";
    }

    @PostMapping("/reviews/{id}")
    public String update(@PathVariable int id,
            @RequestParam(required = false) String from,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam int rating,
            @RequestParam String description,
            Model model) {
        try {
            reviewService.update(id, currentUser.getId(), rating, description);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("review", reviewService.getOwnedReview(id, currentUser.getId()));
            model.addAttribute("from", from);
            model.addAttribute("currentUser", currentUser);
            return "reviews/edit";
        }
        return redirectAfter(from, currentUser);
    }

    private String redirectAfter(String from, AuthenticatedUser currentUser) {
        return "profile".equals(from) ? "redirect:/users/" + currentUser.getId() : "redirect:/"; // /reviews -> homepage  /
    }

    @GetMapping("/reviews/new")
    public String form(@AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        model.addAttribute("currentUser", currentUser);

        final boolean isPrivate = userService.getUserById(currentUser.getId())
                .map(User::isPrivateAccount).orElse(false);
        model.addAttribute("isPrivateAccount", isPrivate);

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
        }
        catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("currentUser", currentUser);

            final boolean isPrivate = userService.getUserById(currentUser.getId())
                .map(User::isPrivateAccount).orElse(false);
            model.addAttribute("isPrivateAccount", isPrivate);

            return "reviews/form";
        }
        return "redirect:/"; // /reviews -> homepage  /
    }
}
