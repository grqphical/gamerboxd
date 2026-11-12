package com.gamerboxd.gamerboxd.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.services.SteamService;
import com.gamerboxd.gamerboxd.services.UserService;
import com.gamerboxd.gamerboxd.services.SteamService.SteamProfile;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class SteamLoginController {

    private final SteamService steamService;
    private final UserService userService;

    @Value("${app.base-url}")
    private String baseUrl;

    public SteamLoginController(SteamService steamService, UserService userService) {
        this.steamService = steamService;
        this.userService = userService;
    }

    /*
     * Sends the user to sign in with Steam, if an account doesn't exist with
     * Gamerboxd, we create an account. Otherwise just log the user in
     */
    @GetMapping("/auth/steam")
    public ResponseEntity<Void> signInWithSteam(HttpServletRequest request) {
        return this.steamService.getSteamSignInRedirect(request, baseUrl);
    }

    @GetMapping("/auth/steam/callback")
    public String steamAuthCallback(HttpServletRequest request, RedirectAttributes attrs) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            if (e.getKey().startsWith("openid.")) {
                params.add(e.getKey(), e.getValue()[0]);
            }
        }

        if (!"id_res".equals(params.getFirst("openid.mode"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Login cancelled or failed");
        }

        String expectedReturnTo = baseUrl + "/auth/steam/callback";
        if (!expectedReturnTo.equals(params.getFirst("openid.return_to"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "return_to mismatch");
        }

        String steamId = this.steamService.verifySteamLogin(params);

        attrs.addAttribute("steamid", steamId);
        return "redirect:/auth/steam/finish";
    }

    @GetMapping("/auth/steam/finish")
    public String steamSignInFinish(@AuthenticationPrincipal AuthenticatedUser currentUser, HttpServletRequest request,
            Model model, @RequestParam("steamid") String steamId) {
        SteamProfile profile = this.steamService.fetchProfile(steamId);

        if (profile.name() == "") {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to retrieve profile");
        }

        if (currentUser != null) {
            // user is already logged into an account so we just need to link their Steam
            // account to the current user
            this.userService.linkSteamAccount(steamId, currentUser.getId());
        }

        // If user already has an account linked to steam with us, just log them in
        // manually
        if (this.userService.hasLinkedSteamAccount(steamId)) {
            this.userService.loginWithSteam(steamId, request);
            return "redirect:/";
        }

        model.addAttribute("profile", profile);

        return "steam/accountSetup";
    }

    @PostMapping("/auth/steam/finish")
    public String steamSignInFinishFormHandler(
            @RequestParam String steamID,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {

        try {
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }
            userService.registerWithSteam(username, email, password, steamID);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "signup";
        }
        return "redirect:/login?registered";

    }
}
