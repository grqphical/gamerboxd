package com.gamerboxd.gamerboxd.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserPrivacy;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.ReviewService;
import com.gamerboxd.gamerboxd.services.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;


@Controller
public class SettingsController {
    private final UserService userService;

    public SettingsController(UserService userService, ReviewService reviewService) {
        this.userService = userService;
    }

    @GetMapping("/users/{id}/settings")
    public String settingsPage(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable int id, Model model) {
        User user = userService.getUserById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "User not found with ID: " + id));

        if (currentUser == null) { return "redirect:/login"; }
        int userId = currentUser.getId();
        if (userId != id) { return "redirect:/users/" + id; }
        
        model.addAttribute("user", user);

        return "users/settings";
    }

    /**
     * request should have these fields:
     * - "username"
     * - "email"
     * - "password"
     * - "privacy"
     * - "description"
     * - "pronouns"
     * 
     * does param verifying here but also happens in UserService. might be better to make
     * one verification method/function inside User that returns an optional error message
     */
    @PostMapping("users/{id}/settings")
    public String updateSettings(@PathVariable int id, Model model, @RequestParam Map<String, String> request) {
        final String ERROR_PATH = "users/settings";
        final String HAPPY_PATH = "/users/" + id;

        Optional<User> userOpt = userService.getUserById(id);
        
        // shouldnt ever happen
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found with ID: " + id);
        }

        User user = userOpt.get();
        model.addAttribute("user", user);

        String errorMessage = "";

        final String username = request.get("username");
        final String email = request.get("email");
        final String password = request.get("password");

        try {
            userService.validateWithLongException(username, email, password);
        }
        catch (IllegalArgumentException exception) {
            errorMessage += exception.getMessage();
        }

        // front end should only list "private" and "public"
        final UserPrivacy privacy = request.get("private") == "private" 
                                  ? UserPrivacy.PRIVATE : UserPrivacy.PUBLIC;
        
        final String description = request.get("description") != null 
                                 ? request.get("description") : "";

        if (description.length() > User.MAX_DESCRIPTION_LENGTH) {
            errorMessage += "- Description must be shorter than " + 
                            User.MAX_DESCRIPTION_LENGTH + "characters.\n";
        }

        // front end should only allow postings of this format
        final String pronouns = request.get("pronouns") != null ? request.get("pronouns") : "";
        if (!pronouns.isBlank() && pronouns.matches("[a-zA-z]+/[a-zA-Z")) {
            errorMessage += "- Invalid pronoun format.\n";
        }

        if (errorMessage.isBlank()) {
            model.addAttribute("error", errorMessage);
            return ERROR_PATH;
        }

        // at this point everything should be valid

        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setPrivacy(privacy);
        user.setDescription(description);
        user.setPronouns(pronouns);

        return HAPPY_PATH;
    }
}