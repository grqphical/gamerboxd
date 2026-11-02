package com.gamerboxd.gamerboxd.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.repositories.UserRepository;
import com.gamerboxd.gamerboxd.services.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@Controller 
public class AdminPanelController {
    private final UserService userService;

    public AdminPanelController(UserService userService) {
        this.userService = userService;
    }

    private boolean adminCheck(AuthenticatedUser currentUser) {
        return currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/admin")
    public String adminPanel(@AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        if (currentUser == null) { return "redirect:/login"; }
        if (!adminCheck(currentUser)) { return "redirect:/"; }

        model.addAttribute("users", userService.getAllUsers());
        return "admin";
    }

    @PostMapping("/admin/{id}/delete")
    public String deleteUser(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable int id) {
        if (currentUser == null) { return "redirect:/login"; }
        if (!adminCheck(currentUser)) { return "redirect:/"; }
        userService.deleteUser(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/{id}/toggleRole")
    public String toggleRole(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable int id) {
        if (currentUser == null) { return "redirect:/login"; }
        if (!adminCheck(currentUser)) { return "redirect:/"; }
        userService.toggleRole(id);
        return "redirect:/admin";
    }
    
    @PostMapping("/admin/{id}/editDescription")
    public String editDescription(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable int id, @RequestParam(defaultValue = "") String description) {
        if (currentUser == null) { return "redirect:/login"; }
        if (!adminCheck(currentUser)) { return "redirect:/"; }
        userService.editDescription(id, description);
        return "redirect:/admin";
    }
    
    @PostMapping("/admin/createUser")
    public String createUser(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @RequestParam String username,
        @RequestParam String email,
        @RequestParam String password,
        @RequestParam String role,
        @RequestParam String privacy,
        @RequestParam String description,
        @RequestParam String pronouns
    ) {
        if (currentUser == null) { return "redirect:/login"; }
        if (!adminCheck(currentUser)) { return "redirect:/"; }
        
        User user = userService.register(username, email, password);
        userService.updateSettings(user, username, email, password, privacy, description, pronouns);
        return "redirect:/admin";
    }
    
}
