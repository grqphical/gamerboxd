package com.gamerboxd.gamerboxd.controllers;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.ReviewCommentService;
import com.gamerboxd.gamerboxd.services.ReviewService;

@Controller
public class ReviewCommentController {
    private final ReviewService reviewService;
    private final ReviewCommentService reviewCommentService;
    private final GameService gameService;

    public ReviewCommentController(ReviewService reviewService, ReviewCommentService reviewCommentService,
            GameService gameService) {
        this.reviewService = reviewService;
        this.reviewCommentService = reviewCommentService;
        this.gameService = gameService;
    }

    @GetMapping("/reviews/{id}")
    public String reviewPage(@PathVariable int id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            Model model) {
        populate(model, id, currentUser);
        return "reviews/detail";
    }

    @PostMapping("/reviews/{id}/comments")
    public String addComment(@PathVariable int id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam String content,
            Model model) {
        try {
            reviewCommentService.add(id, currentUser.getId(), content);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            populate(model, id, currentUser);
            return "reviews/detail";
        }
        return "redirect:/reviews/" + id;
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable int id,
            @RequestParam int reviewId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        reviewCommentService.delete(id, currentUser.getId());
        return "redirect:/reviews/" + reviewId;
    }

    private void populate(Model model, int reviewId, AuthenticatedUser currentUser) {
        Integer viewerId = currentUser == null ? null : currentUser.getId();
        Review review = reviewService.getVisibleReview(reviewId, viewerId);
        model.addAttribute("review", review);
        model.addAttribute("gameName", gameService.getGameNames(List.of(review.getGameId())).get(review.getGameId()));
        model.addAttribute("comments", reviewCommentService.getComments(reviewId));
        model.addAttribute("currentUser", currentUser);
    }
}
