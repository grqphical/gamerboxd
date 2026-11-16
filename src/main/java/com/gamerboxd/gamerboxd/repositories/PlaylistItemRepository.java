package com.gamerboxd.gamerboxd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamerboxd.gamerboxd.models.PlaylistItem;

public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Integer> {
    // In the order the games were added
    List<PlaylistItem> findAllByPlaylistIdOrderById(int playlistId);

    boolean existsByPlaylistIdAndGameId(int playlistId, int gameId);
}
