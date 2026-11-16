package com.gamerboxd.gamerboxd.models;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "PlaylistLikes",
    indexes = @Index(name = "idx_playlist_likes_user", columnList = "user_id"),
    uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "user_id"}))
public class PlaylistLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Playlist playlist;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    public PlaylistLike() {}

    public PlaylistLike(Playlist playlist, User user) {
        this.playlist = playlist;
        this.user = user;
    }

    public int getId() { return id; }
    public Playlist getPlaylist() { return playlist; }
    public User getUser() { return user; }
}
