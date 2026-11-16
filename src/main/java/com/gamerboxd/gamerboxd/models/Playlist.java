package com.gamerboxd.gamerboxd.models;

import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A user's named list of games. The games live in {@link PlaylistItem}. */
@Entity
@Table(name = "Playlists", indexes = @Index(name = "idx_playlists_user", columnList = "user_id"))
public class Playlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Deleting a user removes their playlists at the database level
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private Instant createdAt = Instant.now();

    public Playlist() {}

    public Playlist(User user, String title, String description) {
        this.user = user;
        this.title = title;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public User getUser() { return user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
}
