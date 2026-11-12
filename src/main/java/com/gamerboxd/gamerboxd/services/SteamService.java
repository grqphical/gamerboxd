package com.gamerboxd.gamerboxd.services;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class SteamService {
    private final UserService userService;
    private final RestClient rest;

    private static String STEAM_OPENID_URL = "https://steamcommunity.com/openid/login";
    private static String STEAM_AUTH_CALLBACK_ENDPOINT = "/auth/steam/callback";

    // the regex to extract the user's steam ID
    private static final Pattern CLAIMED_ID = Pattern.compile("^https://steamcommunity\\.com/openid/id/(\\d{17})$");

    public static class SteamAuthException extends RuntimeException {
        public SteamAuthException(String message) {
            super(message);
        }

        public SteamAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public SteamService(UserService userService) {
        this.userService = userService;
        this.rest = RestClient.create();
    }

    /*
     * Returns the response that should be returned by the HTTP handler that sends
     * the user to Steam to sign-in
     */
    public ResponseEntity<Void> getSteamSignInRedirect(HttpServletRequest request, String baseURL) {
        URI steamURL = UriComponentsBuilder
                .fromUriString("https://steamcommunity.com/openid/login")
                .queryParam("openid.ns", "http://specs.openid.net/auth/2.0")
                .queryParam("openid.mode", "checkid_setup")
                .queryParam("openid.return_to", baseURL + STEAM_AUTH_CALLBACK_ENDPOINT)
                .queryParam("openid.realm", baseURL)
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

    /*
     * Asks steam if the login is valid. Just ensures users can't spoof the steam
     * sign-in. Returns the user's steam ID on success, otherwise it throws an
     * exception.
     */
    public String verifySteamLogin(MultiValueMap<String, String> params) {
        // We need to use the same parameters Steam sent back from the first
        // authentication
        // but we need to tell Steam we are checking for authentication
        params.set("openid.mode", "check_authentication");

        String verifyBody = rest.post()
                .uri(STEAM_OPENID_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(String.class);

        if (verifyBody == null || !verifyBody.contains("is_valid:true")) {
            throw new SteamAuthException("Steam could not verify authentication");
        }

        String claimedId = params.getFirst("openid.claimed_id");
        Matcher m = CLAIMED_ID.matcher(claimedId == null ? "" : claimedId);
        if (!m.matches()) {
            throw new SteamAuthException("Bad claimed_id");
        }
        String steamId = m.group(1);
        return steamId;
    }
}
