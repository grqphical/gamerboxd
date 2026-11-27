package com.gamerboxd.gamerboxd.services;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import com.gamerboxd.gamerboxd.models.Game;

/**
 * Class responsible for translating IGDB game database entries into our own
 * Game model.
 */

@Service
public class IGDBService {

    private static final String FIELDS = "fields name,summary,rating,rating_count,first_release_date,genres.name,artworks.image_id,platforms.name,cover.image_id,themes.name;";
    private static final int IGDB_MAX_LIMIT = 500; // 500 is the max size. does not mean we get 500 results

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
            // An error here means the token is no longer valid
            tokens.invalidate();
            return send(endpoint, body, type);
        } catch (HttpClientErrorException.TooManyRequests e) {
            // we got rate limited :(
            // wait and try again
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            }

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

    /**
     * @param name name of the game to search for.
     * @param limit number of games to search for.
     * @param offset number of games to skip. */
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

    /**
     * Fetch many games in as few IGDB requests as possible. Unknown ids are
     * silently omitted.
     */
    public List<Game> getGames(Collection<Integer> ids) {
        List<Integer> distinct = ids.stream().distinct().toList();
        List<Game> result = new ArrayList<>();

        for (int i = 0; i < distinct.size(); i += IGDB_MAX_LIMIT) {
            List<Integer> chunk = distinct.subList(i, Math.min(i + IGDB_MAX_LIMIT, distinct.size()));
            String idList = chunk.stream().map(String::valueOf).collect(Collectors.joining(","));
            String query = FIELDS + " where id = (" + idList + "); limit " + chunk.size() + ";";
            result.addAll(fetchGames(query));
        }
        return result;
    }

    public Game getGameBySteamAppId(int appId, String gameName) {
        String query = "fields game; where external_game_source = 1 & uid = \"" + appId + "\"; limit 1;";
        List<JsonNode> nodes = post("/external_games", query,
                new ParameterizedTypeReference<List<JsonNode>>() {
                });

        if (nodes.isEmpty() || !nodes.get(0).hasNonNull("game")) {
            List<Game> searchResults = this.getSearchResults(gameName, 1);
            if (searchResults.size() == 0)
                return null;
            else
                return searchResults.get(0);
        }
        return getGame(nodes.get(0).get("game").asInt());
    }

    private List<Game> fetchGames(String query) {
        List<JsonNode> nodes = post("/games", query,
                new ParameterizedTypeReference<List<JsonNode>>() {
                });
        return nodes.stream().map(this::toGame).toList();
    }

    private Game toGame(JsonNode n) {
        int id = n.get("id").asInt();
        String name = n.hasNonNull("name") ? n.get("name").asString() : null;
        String summary = n.hasNonNull("summary") ? n.get("summary").asString() : null;
        Double rating = n.hasNonNull("rating") ? n.get("rating").asDouble() : null;
        int numRatings = n.path("rating_count").asInt(0);

        String releaseDate = null;
        if (n.hasNonNull("first_release_date")) {
            releaseDate = Instant.ofEpochSecond(n.get("first_release_date").asLong())
                    .atZone(ZoneOffset.UTC).toLocalDate().toString(); // e.g. "2017-03-03"
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

        List<String> platforms = new ArrayList<>();
        if (n.hasNonNull("platforms")) {
            for (JsonNode g : n.get("platforms")) {
                platforms.add(g.get("name").asString());
            }
        }

        String cover = n.hasNonNull("cover")
                ? "https://images.igdb.com/igdb/image/upload/t_cover_big/" + n.get("cover").get("image_id").asString()
                        + ".webp"
                : null;
        
        List<String> themes = new ArrayList<>();
        if (n.hasNonNull("themes")) {
            for (JsonNode g : n.get("themes")) {
                themes.add(g.get("name").asString());
            }
        }

        return new Game(id, name, summary, releaseDate, rating, numRatings, null, genres, artworks, platforms, cover, themes);
    }
}