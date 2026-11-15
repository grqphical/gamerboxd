package com.gamerboxd.gamerboxd.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "Users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id; // not modifiable in settings

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    private String password; // modifiable in settings with an "are you sure?"
    private UserRole role;

    // get assigned default values on initialization

    private UserPrivacy privacy; // public by default, private
    private String description; // 250 char limit "" by default
    private String pronouns; // "x / y" format or ""

    @Column(nullable = true)
    private String steamID;

    @ElementCollection 
    @OrderColumn 
    private List<Integer> favouriteIgdbIDs = 
        new ArrayList<>(Collections.nCopies(MAX_FAVOURITE_GAMES, 0)); // initialized to MAX_FAVOURITE_GAMES copies of 0.

    public static final int MAX_DESCRIPTION_LENGTH = 250;
    public static final int MAX_FAVOURITE_GAMES = 5; // safe to change, lowering will drop the lowest ranked favourites
    public static final int NULL_FAVOURITE_GAME = 0;

    public User() {
    }

    public User(
            int id, String username, String email,
            String password, UserRole role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;

        this.privacy = UserPrivacy.PUBLIC;
        this.description = "";
        this.pronouns = "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserPrivacy getPrivacy() {
        return privacy;
    }

    public void setPrivacy(UserPrivacy privacy) {
        this.privacy = privacy;
    }

    public boolean isPrivateAccount() {
        return privacy == UserPrivacy.PRIVATE;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPronouns() {
        return pronouns;
    }

    public void setPronouns(String pronouns) {
        this.pronouns = pronouns;
    }

    public String getSteamID() {
        return steamID;
    }

    public void setSteamID(String steamID) {
        this.steamID = steamID;
    }

    /** 
     * Read-only copy that is always {@code MAX_FAVOURITE_GAMES} long. 
     * 
     * To modify this `User`s favourite Ids please use 
     * {@code .setFavouriteIgdbIDs} or {@code .setFavouriteIgdbID}. 
     */
    public List<Integer> getFavouriteIgdbIDs() {
        List<Integer> ids = new ArrayList<>(favouriteIgdbIDs.stream()
            .filter(id -> id != NULL_FAVOURITE_GAME)
            .limit(MAX_FAVOURITE_GAMES)
            .toList());

        while (ids.size() < MAX_FAVOURITE_GAMES) {
            ids.add(NULL_FAVOURITE_GAME);
        }   

        return List.copyOf(ids);
    }

    /**
     * @param ids List of up to {@code MAX_FAVOURITE_GAMES} IGDB ids. 
     * Cannot be duplicates except for multiple 0s meaning "no favourite".
     * A list shorter than {@code MAX_FAVOURITE_GAMES} will fill the remaining 
     * favourite game slots with 0.
     * 
     * @throws IllegalArgumentException {@code if (ids.size() > User.MAX_FAVOURITE_GAMES)}
     * @throws IllegalArgumentException If duplicate igdbIDs in {@code ids}, 
     * with the exception of multiple ids of 0 which is allowed.
     */
    public void setFavouriteIgdbIDs(List<Integer> ids) {
        final int size = ids.size();
        if (size > MAX_FAVOURITE_GAMES) {
            throw new IllegalArgumentException(
                "setFavouriteIgdbIDs: New list has " + size + 
                " elements; greater than maximum " + MAX_FAVOURITE_GAMES + ".");
        }

        List<Integer> noZeros = ids.stream().filter(id -> id != NULL_FAVOURITE_GAME).toList();
        if (noZeros.stream().distinct().count() != noZeros.size()) {
            throw new IllegalArgumentException(
                "setFavouriteIgdbIDs: Duplicate games in favourites!");
        }

        this.favouriteIgdbIDs.clear();
        this.favouriteIgdbIDs.addAll(noZeros);
        while (this.favouriteIgdbIDs.size() < MAX_FAVOURITE_GAMES) {
            this.favouriteIgdbIDs.add(NULL_FAVOURITE_GAME);
        }
    }

    /**
     * @param index 0-indexed. Slot of game to modify ie #2 favourite game: index = 1. 
     * If the ids before index are 0 then the actual igbd id being set might end up
     * at an earlier index.
     * @param igdbID Id of the game which is the new favourite.
     * 
     * @throws ArrayIndexOutOfBoundsException If index not in [0, {@code MAX_FAVOURITE_GAMES})
     */
    public void setFavouriteIgdbID(int index, int igdbID) {
        if (index < 0 || index >= MAX_FAVOURITE_GAMES) {
            throw new ArrayIndexOutOfBoundsException(
                "setFavouriteIgdbID: " + index + " not in [0, " + MAX_FAVOURITE_GAMES + ").");
        }

        List<Integer> updated = new ArrayList<>(getFavouriteIgdbIDs());

        updated.set(index, igdbID);
        setFavouriteIgdbIDs(updated);
    }
}
