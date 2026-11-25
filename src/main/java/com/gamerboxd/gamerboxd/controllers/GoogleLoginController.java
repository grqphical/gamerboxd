package com.gamerboxd.gamerboxd.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.services.GoogleAuthService;
import com.gamerboxd.gamerboxd.services.GoogleAuthService.PendingGoogleAccount;
import com.gamerboxd.gamerboxd.services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The Gamerboxd side of Google sign-in. The exchange with Google itself is
 * Spring Security's: /oauth2/authorization/google starts it and
 * GoogleLoginSuccessHandler picks up the result.
 */
@Controller
public class GoogleLoginController {
    private static final String GOOGLE_LOGIN = "redirect:/oauth2/authorization/google";

    private final UserService userService;
    private final GoogleAuthService googleAuthService;

    public GoogleLoginController(UserService userService, GoogleAuthService googleAuthService) {
        this.userService = userService;
        this.googleAuthService = googleAuthService;
    }

    /** New Google users pick a username here before their account is created. */
    @GetMapping("/auth/google/finish")
    public String finishForm(HttpServletRequest request, Model model) {
        PendingGoogleAccount pending = googleAuthService.getPendingAccount(request);
        if (pending == null) {
            return "redirect:/login";
        }

        model.addAttribute("email", pending.email());
        model.addAttribute("username", googleAuthService.suggestUsername(pending));
        return "google/accountSetup";
    }

    @PostMapping("/auth/google/finish")
    public String finish(@RequestParam String username, HttpServletRequest request, HttpServletResponse response,
            Model model) {
        PendingGoogleAccount pending = googleAuthService.getPendingAccount(request);
        if (pending == null) {
            return "redirect:/login";
        }

        User user;
        try {
            user = userService.registerWithGoogle(username, pending.email(), pending.googleId());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", pending.email());
            model.addAttribute("username", username);
            return "google/accountSetup";
        }

        googleAuthService.clearPendingAccount(request);
        googleAuthService.signIn(user, request, response);
        return "redirect:/";
    }

    /**
     * Sends a signed-in user to Google, the success handler links the account when
     * they come back.
     */
    @GetMapping("/auth/google/link")
    public String link(@AuthenticationPrincipal AuthenticatedUser currentUser, HttpServletRequest request) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        googleAuthService.startLinking(request, currentUser.getId());
        return GOOGLE_LOGIN;
    }

    @PostMapping("/auth/google/unlink")
    public String unlink(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (currentUser == null) {
            return "redirect:/login";
        }
        userService.unlinkGoogleAccount(currentUser.getId());
        return "redirect:/users/" + currentUser.getId() + "/settings";
    }
}
