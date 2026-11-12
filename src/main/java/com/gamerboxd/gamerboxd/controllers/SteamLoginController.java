package com.gamerboxd.gamerboxd.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.gamerboxd.gamerboxd.services.SteamService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class SteamLoginController {

    private final SteamService steamService;

    public SteamLoginController(SteamService steamService) {
        this.steamService = steamService;
    }

    /*
     * Sends the user to sign in with Steam, if an account doesn't exist with
     * Gamerboxd, we create an account. Otherwise just log the user in
     */
    @GetMapping("/auth/steam")
    public ResponseEntity<Void> signInWithSteam(HttpServletRequest request) {
        return this.steamService.getSteamSignInRedirect(request);
    }
}
