package com.gamerboxd.gamerboxd.controllers;

import java.util.Optional;
import java.util.Set;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.services.ReviewService;

import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.ReviewCommentService;
import com.gamerboxd.gamerboxd.services.ReviewLikeService;
import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.models.Review;


@Controller
public class GamerboxdController {
    private final ReviewService reviewService;
    private final GameService gameService;
    private final ReviewLikeService reviewLikeService;
    private final ReviewCommentService reviewCommentService;

    public GamerboxdController(ReviewService reviewService,
        GameService gameService,
        ReviewLikeService reviewLikeService,
        ReviewCommentService reviewCommentService) {
        this.reviewService = reviewService;
        this.gameService = gameService;
        this.reviewLikeService = reviewLikeService;
        this.reviewCommentService = reviewCommentService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        // dont show private reviews on the index
        // temporary solution until later iterations which will need to consider following/followers
        List<Review> reviews = reviewService.getAll().stream()
            .filter(r -> !r.getUser().isPrivateAccount())
            .toList();
        List<Integer> reviewIds = reviews.stream().map(Review::getId).toList();
        List<Integer> gameIds = reviews.stream().map(Review::getGameId).toList();

        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewGames", gameService.getGamesByIDs(gameIds)); // covers and names for the feed
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("likeCounts", reviewLikeService.getLikeCounts(reviewIds));
        model.addAttribute("commentCounts", reviewCommentService.getCommentCounts(reviewIds));
        model.addAttribute("likedIds", currentUser == null
                ? Set.<Integer>of()
                : reviewLikeService.getLikedReviewIds(reviewIds, currentUser.getId()));
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
