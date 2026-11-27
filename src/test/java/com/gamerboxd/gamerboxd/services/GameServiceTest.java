package com.gamerboxd.gamerboxd.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.repositories.GameRepository;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {
    @Mock
    private GameRepository gameRepository;

    @Mock
    private IGDBService igdbService;

    @InjectMocks
    private GameService gameService;

    private Game game(int id, String name) {
        return new Game(id, name, null, null, 80.0, 0, null, List.of(), List.of(), List.of(), null);
    }

    @Test
    void getGameByID_cached_skipsIgdb() {
        Game cached = game(7, "Portal");
        when(gameRepository.findById(7)).thenReturn(Optional.of(cached));

        assertSame(cached, gameService.getGameByID(7).orElseThrow());
        verify(igdbService, never()).getGame(anyInt());
    }

    @Test
    void getGameByID_notCached_fetchesAndCaches() {
        Game fetched = game(7, "Portal");
        when(gameRepository.findById(7)).thenReturn(Optional.empty());
        when(igdbService.getGame(7)).thenReturn(fetched);
        when(gameRepository.save(fetched)).thenReturn(fetched);

        assertSame(fetched, gameService.getGameByID(7).orElseThrow());
        verify(gameRepository).save(fetched);
    }

    @Test
    void getGameByID_unknownToIgdb_isEmptyAndNotCached() {
        when(gameRepository.findById(7)).thenReturn(Optional.empty());
        when(igdbService.getGame(7)).thenReturn(null);

        assertTrue(gameService.getGameByID(7).isEmpty());
        verify(gameRepository, never()).save(any());
    }

    @Test
    void getGameByID_igdbReturnsDifferentGame_throws() {
        when(gameRepository.findById(7)).thenReturn(Optional.empty());
        when(igdbService.getGame(7)).thenReturn(game(8, "Portal 2"));

        assertThrows(IllegalStateException.class, () -> gameService.getGameByID(7));
        verify(gameRepository, never()).save(any());
    }

    @Test
    void getGameBySteamAppID_alreadyCached_returnsCachedCopy() {
        Game cached = game(7, "Portal");
        when(igdbService.getGameBySteamAppId(400, "Portal")).thenReturn(game(7, "Portal"));
        when(gameRepository.findById(7)).thenReturn(Optional.of(cached));

        assertSame(cached, gameService.getGameBySteamAppID(400, "Portal").orElseThrow());
        verify(gameRepository, never()).save(any());
    }

    @Test
    void getGameBySteamAppID_newGame_isCached() {
        Game fetched = game(7, "Portal");
        when(igdbService.getGameBySteamAppId(400, "Portal")).thenReturn(fetched);
        when(gameRepository.findById(7)).thenReturn(Optional.empty());
        when(gameRepository.save(fetched)).thenReturn(fetched);

        assertSame(fetched, gameService.getGameBySteamAppID(400, "Portal").orElseThrow());
    }

    @Test
    void getGameBySteamAppID_noMatch_isEmpty() {
        when(igdbService.getGameBySteamAppId(400, "Portal")).thenReturn(null);

        assertTrue(gameService.getGameBySteamAppID(400, "Portal").isEmpty());
    }

    @Test
    void search_mergesOurRatingIntoCachedResultsOnly() {
        Game cached = game(7, "Portal");
        cached.updateRatingGamerboxd(4);
        when(igdbService.getSearchResults("portal", 10)).thenReturn(List.of(game(7, "Portal"), game(8, "Portal 2")));
        when(gameRepository.findAllById(List.of(7, 8))).thenReturn(List.of(cached));

        List<Game> results = gameService.getSearchResultsByName("portal", 10);

        assertEquals(List.of(7, 8), results.stream().map(Game::getId).toList());
        assertEquals(4.0, results.get(0).getRatingGamerboxd());
        assertNull(results.get(1).getRatingGamerboxd());
    }

    @Test
    void updateGameRating_adding_savesNewAverage() {
        Game cached = game(7, "Portal");
        when(gameRepository.findById(7)).thenReturn(Optional.of(cached));

        gameService.updateGameRating(7, 4, true);
        gameService.updateGameRating(7, 2, true);

        assertEquals(3.0, cached.getRatingGamerboxd(), 1e-9);
        assertEquals(2, cached.getNumRatingsGamerboxd());
        verify(gameRepository, times(2)).save(cached);
    }

    @Test
    void updateGameRating_removing_savesNewAverage() {
        Game cached = game(7, "Portal");
        cached.updateRatingGamerboxd(4);
        cached.updateRatingGamerboxd(2);
        when(gameRepository.findById(7)).thenReturn(Optional.of(cached));

        gameService.updateGameRating(7, 2, false);

        assertEquals(4.0, cached.getRatingGamerboxd(), 1e-9);
        assertEquals(1, cached.getNumRatingsGamerboxd());
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 6 })
    void updateGameRating_outOfRange_throws(int rating) {
        assertThrows(IllegalArgumentException.class, () -> gameService.updateGameRating(7, rating, true));
        verify(gameRepository, never()).save(any());
    }

    @Test
    void updateGameRating_unknownGame_throws() {
        when(gameRepository.findById(7)).thenReturn(Optional.empty());
        when(igdbService.getGame(7)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> gameService.updateGameRating(7, 4, true));
    }

    @Test
    void getGameNames_usesCacheAndFetchesTheRest() {
        Game fetched = game(8, "Portal 2");
        when(gameRepository.findAllById(List.of(7, 8, 9))).thenReturn(List.of(game(7, "Portal")));
        when(gameRepository.findById(8)).thenReturn(Optional.empty());
        when(gameRepository.findById(9)).thenReturn(Optional.empty());
        when(igdbService.getGame(8)).thenReturn(fetched);
        when(igdbService.getGame(9)).thenReturn(null);
        when(gameRepository.save(fetched)).thenReturn(fetched);

        assertEquals(Map.of(7, "Portal", 8, "Portal 2"), gameService.getGameNames(List.of(7, 8, 7, 9)));
        verify(igdbService, never()).getGame(7);
    }
}
