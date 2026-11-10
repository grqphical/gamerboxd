package com.gamerboxd.gamerboxd.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;


@Service
public class IGDBService {

    private final RestClient rest;
    private final TwitchTokenService tokens;
    private final String clientId;

    public record Game(Long id, String name, String summary, Double rating) {}

    public IGDBService(RestClient.Builder builder,
                       TwitchTokenService tokens,
                       @Value("${IGDB_CLIENT_ID}") String clientId) {
        this.rest = builder.baseUrl("https://api.igdb.com/v4").build();
        this.tokens = tokens;
        this.clientId = clientId;
    }

    private <T> T post(String endpoint, String body, ParameterizedTypeReference<T> type) {
        try {
            return send(endpoint, body, type);
        } catch (HttpClientErrorException.Unauthorized e) {
            tokens.refresh();                    // token revoked or expired early
            return send(endpoint, body, type);
        }
    }

    private <T> T send(String endpoint, String body, ParameterizedTypeReference<T> type) {
        return rest.post()
                .uri(endpoint)
                .header("Client-ID", clientId)
                .header("Authorization", "Bearer " + tokens.getToken())
                .contentType(MediaType.TEXT_PLAIN)
                .body(body)
                .retrieve()
                .body(type);
    }
    
    public List<Game> getSearchResults(String name, int limit) {
        String safe = name.replace("\\", "\\\\").replace("\"", "\\\"");
                                                // fields name,*; search "minecraft dungeons";
        String query = "search \"" + safe + "\"; fields name,summary,rating; limit " + limit + ";"; 
        return post("/games", query, new ParameterizedTypeReference<>() {});
    }
}