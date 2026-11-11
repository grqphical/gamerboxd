package com.gamerboxd.gamerboxd.services;

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
}
