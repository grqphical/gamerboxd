package com.gamerboxd.gamerboxd.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class UserTest {
    private final User user = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);

    @Test
    void newUser_hasPublicDefaultsAndEmptyFavourites() {
        assertEquals(UserPrivacy.PUBLIC, user.getPrivacy());
        assertFalse(user.isPrivateAccount());
        assertEquals("", user.getDescription());
        assertEquals("", user.getPronouns());
        assertEquals(List.of(0, 0, 0, 0), user.getFavouriteIgdbIDs());
        assertEquals(Map.of(), user.getGameStatuses());
    }

    @Test
    void setFavourites_padsShortListsWithEmptySlots() {
        user.setFavouriteIgdbIDs(List.of(7, 9));

        assertEquals(List.of(7, 9, 0, 0), user.getFavouriteIgdbIDs());
    }

    @Test
    void setFavourites_movesEmptySlotsToTheEnd() {
        user.setFavouriteIgdbIDs(List.of(0, 7, 0, 9));

        assertEquals(List.of(7, 9, 0, 0), user.getFavouriteIgdbIDs());
    }

    @Test
    void setFavourites_tooMany_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> user.setFavouriteIgdbIDs(List.of(1, 2, 3, 4, 5, 6)));
    }

    @Test
    void setFavourites_duplicates_throwsAndKeepsExisting() {
        user.setFavouriteIgdbIDs(List.of(7));

        assertThrows(IllegalArgumentException.class, () -> user.setFavouriteIgdbIDs(List.of(3, 3)));
        assertEquals(List.of(7, 0, 0, 0), user.getFavouriteIgdbIDs());
    }

    @Test
    void setFavourite_replacesSlot() {
        user.setFavouriteIgdbIDs(List.of(7, 9));

        user.setFavouriteIgdbID(1, 11);

        assertEquals(List.of(7, 11, 0, 0), user.getFavouriteIgdbIDs());
    }

    @Test
    void setFavourite_clearingSlot_closesTheGap() {
        user.setFavouriteIgdbIDs(List.of(7, 9, 11));

        user.setFavouriteIgdbID(0, User.NULL_FAVOURITE_GAME);

        assertEquals(List.of(9, 11, 0, 0), user.getFavouriteIgdbIDs());
    }

    @Test
    void setFavourite_indexOutOfRange_throws() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> user.setFavouriteIgdbID(-1, 7));
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> user.setFavouriteIgdbID(User.MAX_FAVOURITE_GAMES, 7));
    }

    @Test
    void getFavourites_isReadOnly() {
        assertThrows(UnsupportedOperationException.class, () -> user.getFavouriteIgdbIDs().set(0, 7));
    }

    @Test
    void setGameStatus_addsReplacesAndRemoves() {
        user.setGameStatus(7, GameStatus.WANT_TO_PLAY);
        user.setGameStatus(7, GameStatus.PLAYED);
        user.setGameStatus(9, GameStatus.DROPPED);
        assertEquals(Map.of(7, GameStatus.PLAYED, 9, GameStatus.DROPPED), user.getGameStatuses());

        user.setGameStatus(9, null);
        assertEquals(Map.of(7, GameStatus.PLAYED), user.getGameStatuses());
    }

    @Test
    void isPrivateAccount_followsPrivacy() {
        user.setPrivacy(UserPrivacy.PRIVATE);

        assertTrue(user.isPrivateAccount());
    }
}
