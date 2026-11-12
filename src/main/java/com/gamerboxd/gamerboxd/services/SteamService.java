package com.gamerboxd.gamerboxd.services;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class SteamService {
    private final UserService userService;
    private final RestClient rest;

    private static String STEAM_AUTH_CALLBACK_ENDPOINT = "/auth/steam/callback";

    public SteamService(UserService userService) {
        this.userService = userService;
        this.rest = RestClient.builder().baseUrl("http://api.steampowered.com/").build();
    }

    /*
     * Gets what URL the app is being run on. Required by Steam for sign in with
     * steam
     */
    private String getAppHost(HttpServletRequest request) {
        String serverName = request.getServerName(); // "localhost" or "example.com"
        int port = request.getServerPort(); // 8080, 443, ...
        String scheme = request.getScheme(); // "http" / "https"
        return scheme + "://" + serverName + ":" + port;
    }

    /*
     * Returns the response that should be returned by the HTTP handler that sends
     * the user to Steam to sign-in
     */
    public ResponseEntity<Void> getSteamSignInRedirect(HttpServletRequest request) {
        URI steamURL = UriComponentsBuilder
                .fromUriString("https://steamcommunity.com/openid/login")
                .queryParam("openid.ns", "http://specs.openid.net/auth/2.0")
                .queryParam("openid.mode", "checkid_setup")
                .queryParam("openid.return_to", STEAM_AUTH_CALLBACK_ENDPOINT)
                .queryParam("openid.realm", getAppHost(request))
                .queryParam("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select")
                .queryParam("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select")
                .build()
                .encode()
                .toUri();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(steamURL)
                .build();
    }
}
