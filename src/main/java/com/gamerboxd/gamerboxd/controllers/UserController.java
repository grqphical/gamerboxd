package com.gamerboxd.gamerboxd.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.gamerboxd.gamerboxd.models.GameStatus;
import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserPrivacy;
import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.ReviewCommentService;
import com.gamerboxd.gamerboxd.services.ReviewLikeService;
import com.gamerboxd.gamerboxd.services.ReviewService;
import com.gamerboxd.gamerboxd.services.UserService;

@Controller
public class UserController {
    private final UserService userService;
    private final ReviewService reviewService;
    private final GameService gameService;
    private final ReviewLikeService reviewLikeService;
    private final ReviewCommentService reviewCommentService;

    public UserController(UserService userService, ReviewService reviewService, GameService gameService,
            ReviewLikeService reviewLikeService, ReviewCommentService reviewCommentService) {
        this.userService = userService;
        this.reviewService = reviewService;
        this.gameService = gameService;
        this.reviewLikeService = reviewLikeService;
        this.reviewCommentService = reviewCommentService;
    }

    @GetMapping("/users/{id}")
    public String getUserProfile(@PathVariable int id,
            @RequestParam(required = false) String gameSearch,
            @RequestParam(required = false) String statusGameSearch,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            Model model) {

        User user = userService.getUserById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "User not found with ID: " + id));

        if (currentUser == null && user.getPrivacy() == UserPrivacy.PRIVATE) {
            return "redirect:/login";
        }

        List<Review> userReviews = reviewService.getAllByUserId(id);
        List<Integer> reviewIds = userReviews.stream().map(Review::getId).toList();
        List<Integer> gameIds = userReviews.stream().map(Review::getGameId).toList();

        // Collect every game id this page needs (favourites, tracked games and
        // reviewed games) so they can be loaded with one batched lookup instead
        // of one DB query / IGDB request per game.
        Set<Integer> allGameIds = new HashSet<>(user.getFavouriteIgdbIDs());
        allGameIds.remove(User.NULL_FAVOURITE_GAME);
        allGameIds.addAll(user.getGameStatuses().keySet());
        allGameIds.addAll(gameIds);

        Map<Integer, Game> gamesById = gameService.getGamesByIDs(allGameIds);

        List<Game> favouriteGames = new ArrayList<>();
        for (int igdbID : user.getFavouriteIgdbIDs()) {
            if (igdbID == User.NULL_FAVOURITE_GAME) {
                continue;
            }

            Game game = gamesById.get(igdbID);
            if (game != null) {
                favouriteGames.add(game);
            }
        }

        final boolean isOwnProfile = currentUser != null && currentUser.getId() == id;

        // contains ids of 0 meaning there are free slots
        final boolean canAddFavourite = isOwnProfile && user.getFavouriteIgdbIDs().contains(User.NULL_FAVOURITE_GAME);

        final int searchLimit = 10;
        List<Game> gameSearchResults = (canAddFavourite && gameSearch != null && !gameSearch.isBlank())
                ? gameService.getSearchResultsByName(gameSearch, searchLimit) // search doesnt cache results
                : List.of();
        List<Game> statusGameSearchResults = isOwnProfile && statusGameSearch != null && !statusGameSearch.isBlank()
                ? gameService.getSearchResultsByName(statusGameSearch, searchLimit)
                : List.of();

        Map<GameStatus, List<Game>> gamesByStatus = new EnumMap<>(GameStatus.class);
        for (GameStatus status : GameStatus.values()) {
            gamesByStatus.put(status, new ArrayList<>());
        }
        user.getGameStatuses().forEach((igdbID, status) -> {
            Game game = gamesById.get(igdbID);
            if (game != null) {
                gamesByStatus.get(status).add(game);
            }
        });

        Map<Integer, String> reviewGameNames = new HashMap<>();
        for (int gameId : gameIds) {
            Game game = gamesById.get(gameId);
            if (game != null && game.getName() != null) {
                reviewGameNames.put(gameId, game.getName());
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("reviews", userReviews);
        model.addAttribute("favouriteGames", favouriteGames);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("canAddFavourite", canAddFavourite);
        model.addAttribute("gameSearch", gameSearch);
        model.addAttribute("gameSearchResults", gameSearchResults);
        model.addAttribute("statusGameSearch", statusGameSearch);
        model.addAttribute("statusGameSearchResults", statusGameSearchResults);
        model.addAttribute("gamesByStatus", gamesByStatus);
        model.addAttribute("gameStatuses", GameStatus.values());
        model.addAttribute("likeCounts", reviewLikeService.getLikeCounts(reviewIds));
        model.addAttribute("commentCounts", reviewCommentService.getCommentCounts(reviewIds));
        model.addAttribute("reviewGameNames", reviewGameNames);
        model.addAttribute("likedIds", currentUser == null
                ? Set.<Integer>of()
                : reviewLikeService.getLikedReviewIds(reviewIds, currentUser.getId()));

        return "users/profile";
    }

    @PostMapping("/users/{id}/games/status")
    public String updateGameStatus(@PathVariable int id,
            @RequestParam int igdbID,
            @RequestParam GameStatus status,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        User user = getOwnUser(id, currentUser);
        if (gameService.getGameByID(igdbID).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found with ID: " + igdbID);
        }

        user.setGameStatus(igdbID, status);
        userService.save(user);
        return "redirect:/users/" + user.getId();
    }

    @PostMapping("/users/{id}/games/remove")
    public String removeGameStatus(@PathVariable int id,
            @RequestParam int igdbID,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        User user = getOwnUser(id, currentUser);
        user.setGameStatus(igdbID, null);
        userService.save(user);
        return "redirect:/users/" + user.getId();
    }

    private User getOwnUser(int id, AuthenticatedUser currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (currentUser.getId() != id) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return userService.getUserById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "User not found with ID: " + id));
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

    /** Put a igdb game id in the users first available slot if any */
    @PostMapping("/favourites/add")
    public String addFavourite(@RequestParam int igdbID,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        User user = userService.getUserById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with ID: " + currentUser.getId()));

        List<Integer> ids = user.getFavouriteIgdbIDs();
        int emptySlot = ids.indexOf(User.NULL_FAVOURITE_GAME); // index of the first occurrence of 0

        if (emptySlot != -1 && !ids.contains(igdbID) && gameService.getGameByID(igdbID).isPresent()) {
            user.setFavouriteIgdbID(emptySlot, igdbID);
            userService.save(user);
        }

        return "redirect:/users/" + user.getId();
    }

    /** Clear single igdb game id from users favourites list */
    @PostMapping("/favourites/remove")
    public String removeFavourite(@RequestParam int igdbID,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        User user = userService.getUserById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with ID: " + currentUser.getId()));

        int slot = user.getFavouriteIgdbIDs().indexOf(igdbID);
        if (slot != -1) {
            user.setFavouriteIgdbID(slot, User.NULL_FAVOURITE_GAME);
            userService.save(user);
        }

        return "redirect:/users/" + user.getId();
    }

    @PostMapping("/favourites/move")
    public String moveFavourite(@RequestParam int igdbID,
            @RequestParam String direction, // "up" or "down"
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }

        User user = userService.getUserById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with ID: " + currentUser.getId()));

        List<Integer> ids = new ArrayList<>(user.getFavouriteIgdbIDs());
        int fromIndex = ids.indexOf(igdbID);
        int toIndex = (direction.equals("up")) ? fromIndex - 1 : fromIndex + 1;

        if (fromIndex != -1 && toIndex >= 0 && toIndex < ids.size()) {
            Collections.swap(ids, fromIndex, toIndex);
            user.setFavouriteIgdbIDs(ids);
            userService.save(user);
        }

        return "redirect:/users/" + user.getId();
    }
}