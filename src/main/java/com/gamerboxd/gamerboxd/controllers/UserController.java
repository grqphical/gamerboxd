package com.gamerboxd.gamerboxd.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserPrivacy;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.ReviewService;
import com.gamerboxd.gamerboxd.services.UserService;

@Controller
public class UserController {
    private final UserService userService;
    private final ReviewService reviewService;
    private final GameService gameService;

    public UserController(UserService userService, ReviewService reviewService, GameService gameService) {
        this.userService = userService;
        this.reviewService = reviewService;
        this.gameService = gameService;
    }

    @GetMapping("/users/{id}")
    public String getUserProfile(@PathVariable int id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            Model model) {

        User user = userService.getUserById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "User not found with ID: " + id));

        if (currentUser == null && user.getPrivacy() == UserPrivacy.PRIVATE) {
            return "redirect:/login";
        }

        List<Review> userReviews = reviewService.getAllByUserId(id);
        List<Game> favouriteGames = new ArrayList<>();
        for (int igdbID : user.getFavouriteIgdbIDs()) {
            if (igdbID == User.NULL_FAVOURITE_GAME) {
                continue;
            }

            Optional<Game> gameOpt = gameService.getGameByID(igdbID);
            if (gameOpt.isPresent()) {
                favouriteGames.add(gameOpt.get());
            }
        }

        boolean isOwnProfile = currentUser != null && currentUser.getId() == id;

        model.addAttribute("user", user);
        model.addAttribute("reviews", userReviews);
        model.addAttribute("favouriteGames", favouriteGames);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isOwnProfile", isOwnProfile);

        if (user.getRole() == UserRole.ADMIN) {
            // TODO: redirect to admin view
        }

        return "users/profile";
    }

    @GetMapping("/signup")
    public String signupForm(@AuthenticationPrincipal AuthenticatedUser currentUser,
            Model model) {
        model.addAttribute("currentUser", currentUser);
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {
        try {
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }
            userService.register(username, email, password);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "signup";
        }
        return "redirect:/login?registered";
    }
}
