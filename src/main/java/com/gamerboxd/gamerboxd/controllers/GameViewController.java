package com.gamerboxd.gamerboxd.controllers;

import java.util.Set;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.services.ReviewService;

import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.NotificationService;
import com.gamerboxd.gamerboxd.services.ReviewLikeService;
import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.models.Review;

import org.springframework.web.server.ResponseStatusException;

@Controller
public class GameViewController {
        private final GameService gameService;
        private final ReviewService reviewService;
        private final ReviewLikeService reviewLikeService;
        private final NotificationService notificationService;

        public GameViewController(GameService gameService, ReviewService reviewService,
                        ReviewLikeService reviewLikeService, NotificationService notificationService) {
                this.gameService = gameService;
                this.reviewService = reviewService;
                this.reviewLikeService = reviewLikeService;
                this.notificationService = notificationService;
        }

        @GetMapping("/games/{id}")
        public String gameView(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable int id,
                        Model model) {
                Game game = gameService.getGameByID(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Game ID does not match an existing game."));
                List<Review> reviews = reviewService.getAllByGameId(id).stream()
                                .filter(r -> !r.getUser().isPrivateAccount()
                                                || (currentUser != null && r.getUser().getId() == currentUser.getId()))
                                .toList();
                List<Integer> reviewIds = reviews.stream().map(Review::getId).toList();

                model.addAttribute("reviews", reviews);
                model.addAttribute("likeCounts", reviewLikeService.getLikeCounts(reviewIds));
                model.addAttribute("likedIds", currentUser == null
                                ? Set.<Integer>of()
                                : reviewLikeService.getLikedReviewIds(reviewIds, currentUser.getId()));
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("game", game);
                if (currentUser != null)
                        model.addAttribute("notificationCount",
                                        notificationService.getUserNotificationUnreadCount(currentUser.getId()));
                return "games";
        }
}