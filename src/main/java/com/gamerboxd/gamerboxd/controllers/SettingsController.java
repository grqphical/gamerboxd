package com.gamerboxd.gamerboxd.controllers;

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

import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.services.ReviewService;
import com.gamerboxd.gamerboxd.services.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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
     */
    @PostMapping("users/{id}/settings")
    public String updateSettings(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable int id, Model model, @RequestParam Map<String, String> request
    ) {
        if (currentUser == null) { return "redirect:/login"; }
        int userId = currentUser.getId();
        if (userId != id) { return "redirect:/users/" + id; }

        final Optional<User> userOpt = userService.getUserById(id);
        
        // shouldnt ever happen
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found with ID: " + id);
        }

        final User user = userOpt.get();
        model.addAttribute("user", user);

        final String username = request.get("username");    // form warns between 3-30 chars
        final String email = request.get("email");          
        final String password = request.get("password");
        final String privacy = request.get("privacy");      // form dropdown "public/private"
        final String description = request.get("description");  // form warns less than 250 chars
        final String pronouns = request.get("pronouns");    // for only allows "[a-zA-z]+/[a-zA-Z]"

        try {
            userService.updateSettings(
                user, 
                username, email, password, 
                privacy, description, pronouns
            );
        }
        catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage()); // no Null chekc
            return "users/settings";
        }

        return "redirect:/users/" + id;
    }
}