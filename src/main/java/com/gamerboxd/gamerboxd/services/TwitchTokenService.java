package com.gamerboxd.gamerboxd.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
@Component
public class TwitchTokenService {

    private final RestClient rest;
    private final String clientId;
    private final String clientSecret;

    private String token;
    private Instant expiresAt = Instant.EPOCH;

    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {}

    public TwitchTokenService(
        RestClient.Builder builder,
        @Value("${IGDB-CLIENT-ID}") String clientId,
        @Value("${IGDB-CLIENT-SECRET}") String clientSecret) {
            this.rest = builder.build();
            this.clientId = clientId;
            this.clientSecret = clientSecret;
    }

    public synchronized String getToken() {
        // refresh if missing or within 60s of expiry
        if (token == null || Instant.now().isAfter(expiresAt.minusSeconds(60))) {
            refresh();
        }
        return token;
    }

    public synchronized void refresh() {
        final String endpoint = 
            "https://id.twitch.tv/oauth2/token?client_id={id}&client_secret={secret}&grant_type=client_credentials"; 

        TokenResponse response = rest.post()
            .uri(endpoint, clientId, clientSecret)
            .retrieve()
            .body(TokenResponse.class);

        this.token = response.accessToken();
        this.expiresAt = Instant.now().plusSeconds(response.expiresIn());
    }
}
