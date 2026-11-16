package com.gamerboxd.gamerboxd.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamerboxd.gamerboxd.models.PlaylistLike;

public interface PlaylistLikeRepository extends JpaRepository<PlaylistLike, Integer> {
    long countByPlaylistId(int playlistId);

    Optional<PlaylistLike> findByPlaylistIdAndUserId(int playlistId, int userId);
}
