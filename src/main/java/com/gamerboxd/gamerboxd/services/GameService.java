package com.gamerboxd.gamerboxd.services;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;

import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.repositories.GameRepository;

@Service 
public class GameService {
    private final GameRepository gameRepository;
    private final IGDBService igdbService;

    public GameService(GameRepository gameRepository, IGDBService igdbService) {
        this.gameRepository = gameRepository;
        this.igdbService = igdbService;
    } 

    /**
     * Look up a {@code Game} by ID and get back our cached copy if we have one;
     * otherwise get a copy fetched from IGDBService and cache it.
     * 
     * @param igdbID The game's id on IGDB (Should be the same id in the db).
     * @return The game, or empty if this igdbID associates with nothing.
     * @throws IllegalStateException if the fetched game from IGDB has a 
     * differing id from the provided igdbID.
     */
    public Optional<Game> getGameByID(int igdbID) {
        Optional<Game> cachedGame = gameRepository.findById(igdbID);
        if (cachedGame.isPresent()) {
            return cachedGame;
        }

        Game fetchedGame = igdbService.getGame(igdbID);
        if (fetchedGame == null) {
            return Optional.empty();
        }
        
        if (fetchedGame.getId() != igdbID) {
            throw new IllegalStateException(
                "GameService::getGame: IGDB returned game id " + fetchedGame.getId() + 
                " for requested id " + igdbID 
            );
        }

        return Optional.of(gameRepository.save(fetchedGame));
    }
    /**
     * - A wrapper function for the IGDBService function. <br>
     * - Use for searching the IGDB database for a game by name. <br>
     * - Skips the cache to make sure results are unified.
     * @param name the name of the game you want to search
     * @param limit maximum number of search results you want back
    */ 
    public List<Game> getSearchResultsByName(String name, int limit) {
        return igdbService.getSearchResults(name, limit);
    }

    /**
     * Iteratively update the id's associated `Game`'s ratingGamerboxd using:
     * <pre> New Average = Old Average + (x - Old Average)/(n + 1) </pre>
     * @param igdbId The game's id on IGDB (Should be the same id in the db).
     * @param rating The star rating in [0, 5] (star rating).
     * @exception IllegalArgumentException for `rating` not in desired range.
     * @exception IllegalArgumentException for bad `igdbId`.
     */
    public void updateGameRating(int igdbId, int rating) {
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException(
                "GameService::updateGameRating: parameter `rating` " +
                "must be between 0 and 5. Got: "  + rating + "."
            );
        }

        Game game = getGameByID(igdbId).orElseThrow(() -> new IllegalArgumentException(
            "GameService::updateGameRatin: parameter `igdbId` " +
            "doesn't map to any associated `Game`. Got: " + igdbId + "."  
        ));

        game.updateRatingGamerboxd(rating);
        gameRepository.save(game);
    }
}
