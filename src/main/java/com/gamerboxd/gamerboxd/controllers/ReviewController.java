package com.gamerboxd.gamerboxd.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.NotificationService;
import com.gamerboxd.gamerboxd.services.ReviewLikeService;
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
    private final GameService gameService;
    private final ReviewLikeService reviewLikeService;
    private final NotificationService notificationService;

    public ReviewController(
            ReviewService reviewService,
            UserService userService,
            GameService gameService,
            ReviewLikeService reviewLikeService,
            NotificationService notificationService) {
        this.reviewService = reviewService;
        this.userService = userService;
        this.gameService = gameService;
        this.reviewLikeService = reviewLikeService;
        this.notificationService = notificationService;
    }

    @PostMapping("/reviews/{id}/like")
    public String like(@PathVariable int id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) Integer profileId) {
        reviewLikeService.toggleLike(id, currentUser.getId());
        return profileId != null ? "redirect:/users/" + profileId : "redirect:/";
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
        Review review = reviewService.getOwnedReview(id, currentUser.getId());

        model.addAttribute("review", review);
        model.addAttribute("gameName", gameService.getGameNames(List.of(review.getGameId())).get(review.getGameId()));
        model.addAttribute("from", from);
        model.addAttribute("currentUser", currentUser);
        if (currentUser != null)
            model.addAttribute("notificationCount", notificationService.getUserNotificationCount(currentUser.getId()));
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
        return "profile".equals(from) ? "redirect:/users/" + currentUser.getId() : "redirect:/"; // /reviews -> homepage
                                                                                                 // /
    }

    @GetMapping("/reviews/new")
    public String form(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Integer gameId,
            Model model) {
        model.addAttribute("currentUser", currentUser);

        final boolean isPrivate = userService.getUserById(currentUser.getId())
                .map(User::isPrivateAccount).orElse(false);
        model.addAttribute("isPrivateAccount", isPrivate);

        String query = q.trim();
        List<Game> results = List.of();
        Integer selectedGameId = null;

        if (!query.isEmpty()) {
            results = gameService.getSearchResultsByName(query, 10);
        } else if (gameId != null) {
            Optional<Game> game = gameService.getGameByID(gameId);
            if (game.isPresent()) {
                results = List.of(game.get());
                selectedGameId = gameId;
                query = game.get().getName();
            }
        }

        model.addAttribute("query", query);
        model.addAttribute("gameResults", results);
        model.addAttribute("selectedGameId", selectedGameId);
        if (currentUser != null)
            model.addAttribute("notificationCount", notificationService.getUserNotificationCount(currentUser.getId()));
        return "reviews/form";
    }

    @PostMapping("/reviews")
    public String create(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam int gameId,
            @RequestParam int rating,
            @RequestParam String description,
            Model model) {
        try {
            reviewService.create(currentUser.getId(), gameId, rating, description);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("currentUser", currentUser);

            final boolean isPrivate = userService.getUserById(currentUser.getId())
                    .map(User::isPrivateAccount).orElse(false);
            model.addAttribute("isPrivateAccount", isPrivate);

            return "reviews/form";
        }
        return "redirect:/"; // /reviews -> homepage /
    }
}
