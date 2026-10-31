package com.gamerboxd.gamerboxd.controllers;

import java.util.List;

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
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.ReviewService;
import com.gamerboxd.gamerboxd.services.UserService;

@Controller
public class SettingsController {
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

    @PostMapping("users/{id}/settings")
    public String updateSettings(@PathVariable int id, Model model, @RequestParam Map<String, String> request) {
        Optional<User> user = userService.getUserById(id);
        model.addAttribute("user", user);

        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + id);
        }

        final String username = request.get("username");
        final String email = request.get("email");
        final String password = request.get("password");

        try {
            userService.validateWithLongException(username, email, password);
        }
        catch (IllegalArgumentException exception) {
            final String errorMessage = exception.getMessage();
            model.addAttribute("error", errorMessage);
            return "users/settings";
        }

        // update this users settings

        // the happy path
        return "redirect:/users/profile";
    }
}