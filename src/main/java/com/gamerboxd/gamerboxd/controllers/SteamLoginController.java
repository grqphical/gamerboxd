package com.gamerboxd.gamerboxd.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.services.SteamService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class SteamLoginController {

    private final SteamService steamService;

    @Value("${app.base-url}")
    private String baseUrl;

    public SteamLoginController(SteamService steamService) {
        this.steamService = steamService;
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
    public String steamAuthCallback(HttpServletRequest request) {
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

        return "redirect:/";
    }
}
