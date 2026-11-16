package com.gamerboxd.gamerboxd.models;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One game in a playlist, with an optional category and the owner's rating of it. */
@Entity
@Table(name = "PlaylistItems",
    // a game appears at most once per playlist; the constraint also indexes playlist_id
    uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "game_id"}))
public class PlaylistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Playlist playlist;

    private int gameId;

    @Column(length = 50)
    private String category; // null when uncategorised

    private Integer rating; // 1-5, null when unrated. Separate from review ratings.

    public PlaylistItem() {}

    public PlaylistItem(Playlist playlist, int gameId, String category, Integer rating) {
        this.playlist = playlist;
        this.gameId = gameId;
        this.category = category;
        this.rating = rating;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Playlist getPlaylist() { return playlist; }

    public int getGameId() { return gameId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
