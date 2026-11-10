package com.gamerboxd.gamerboxd.services;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import com.gamerboxd.gamerboxd.models.Game;

/**Class responsible for translating IGDB game database entries into our own Game model.*/

@Service
public class IGDBService {

    private static final String FIELDS =
        "fields name,summary,rating,first_release_date,genres.name,artworks.image_id,cover.image_id;";

    private final RestClient rest;
    private final TwitchTokenService tokens;
    private final String clientId;

    public IGDBService(TwitchTokenService tokens,
            @Value("${IGDB_CLIENT_ID}") String clientId) {
        this.rest = RestClient.builder().baseUrl("https://api.igdb.com/v4").build();
        this.tokens = tokens;
        this.clientId = clientId;
    }

    private <T> T post(String endpoint, String body, ParameterizedTypeReference<T> type) {
        try {
            return send(endpoint, body, type);
        } catch (HttpClientErrorException.Unauthorized e) {
            tokens.refresh();
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
    /** Use for searching the IGDB database for a game by name
     * @param name the name of the game you want to search
     * @param limit maximum number of search results you want back
    */ 
    public List<Game> getSearchResults(String name, int limit) {
        String safe = name.replace("\\", "\\\\").replace("\"", "\\\"");
        String query = "search \"" + safe + "\"; " + FIELDS + " limit " + limit + ";";
        return fetchGames(query);
    }

    /** Returns null if IGDB has no game with this id. */
    public Game getGame(int id) {
        String query = FIELDS + " where id = " + id + ";";
        List<Game> result = fetchGames(query);
        return result.isEmpty() ? null : result.get(0);
    }

    private List<Game> fetchGames(String query) {
        List<JsonNode> nodes = post("/games", query,
                new ParameterizedTypeReference<List<JsonNode>>() {});
        return nodes.stream().map(this::toGame).toList();
    }

    private Game toGame(JsonNode n) {
        int id = n.get("id").asInt();
        String name = n.hasNonNull("name") ? n.get("name").asString() : null;
        String summary = n.hasNonNull("summary") ? n.get("summary").asString() : null;
        Double rating = n.hasNonNull("rating") ? n.get("rating").asDouble() : null;

        String releaseDate = null;
        if (n.hasNonNull("first_release_date")) {
            releaseDate = Instant.ofEpochSecond(n.get("first_release_date").asLong())
                    .atZone(ZoneOffset.UTC).toLocalDate().toString();   // e.g. "2017-03-03"
        }

        List<String> genres = new ArrayList<>();
        if (n.hasNonNull("genres")) {
            for (JsonNode g : n.get("genres")) {
                genres.add(g.get("name").asString());
            }
        }

        List<String> artworks = new ArrayList<>();
        if (n.hasNonNull("artworks")) {
            for (JsonNode a : n.get("artworks")) {
                artworks.add("https://images.igdb.com/igdb/image/upload/t_720p/"
                        + a.get("image_id").asString() + ".jpg");
            }
        }

        String cover = n.hasNonNull("cover") ? "https://images.igdb.com/igdb/image/upload/t_cover_big/" + n.get("cover").get("image_id").asString() + ".webp" : null;

        return new Game(id, name, summary, releaseDate, rating, 0.0, genres, artworks, cover);
    }
}