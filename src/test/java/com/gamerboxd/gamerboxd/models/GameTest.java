package com.gamerboxd.gamerboxd.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class GameTest {
    // IGDB knows nothing about our ratings, so fetched games start with null
    private Game unratedGame() {
        return new Game(1, "Game", null, null, null, 0, null, List.of(), List.of(), List.of(), null);
    }

    @Test
    void updateRating_firstRating_becomesTheAverage() {
        Game game = unratedGame();

        game.updateRatingGamerboxd(4);

        assertEquals(4.0, game.getRatingGamerboxd());
        assertEquals(1, game.getNumRatingsGamerboxd());
    }

    @Test
    void updateRating_keepsRunningAverage() {
        Game game = unratedGame();

        game.updateRatingGamerboxd(5);
        game.updateRatingGamerboxd(2);
        game.updateRatingGamerboxd(2);

        assertEquals(3.0, game.getRatingGamerboxd(), 1e-9);
        assertEquals(3, game.getNumRatingsGamerboxd());
    }

    @Test
    void removeRating_restoresPreviousAverage() {
        Game game = unratedGame();
        game.updateRatingGamerboxd(5);
        game.updateRatingGamerboxd(1);

        game.removeRatingGamerboxd(1);

        assertEquals(5.0, game.getRatingGamerboxd(), 1e-9);
        assertEquals(1, game.getNumRatingsGamerboxd());
    }

    @Test
    void removeRating_lastRating_resetsToZero() {
        Game game = unratedGame();
        game.updateRatingGamerboxd(3);

        game.removeRatingGamerboxd(3);

        assertEquals(0.0, game.getRatingGamerboxd());
        assertEquals(0, game.getNumRatingsGamerboxd());
    }

    @Test
    void removeRating_withNoRatings_throws() {
        assertThrows(IllegalStateException.class, () -> unratedGame().removeRatingGamerboxd(3));
    }
}
