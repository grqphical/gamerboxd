package com.gamerboxd.gamerboxd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gamerboxd.gamerboxd.models.Playlist;
import com.gamerboxd.gamerboxd.models.PlaylistSummary;

public interface PlaylistRepository extends JpaRepository<Playlist, Integer> {
    String SUMMARY = """
            select new com.gamerboxd.gamerboxd.models.PlaylistSummary(p.id, p.title, p.description, u.id, u.username,
                (select count(i) from PlaylistItem i where i.playlist = p),
                (select count(l) from PlaylistLike l where l.playlist = p))
            from Playlist p join p.user u
            """;

    // Playlists from public accounts, newest first
    @Query(SUMMARY + "where u.privacy is null or u.privacy <> com.gamerboxd.gamerboxd.models.UserPrivacy.PRIVATE "
            + "order by p.createdAt desc")
    List<PlaylistSummary> findPublicSummaries();

    @Query(SUMMARY + "where u.id = :userId order by p.createdAt desc")
    List<PlaylistSummary> findSummariesByUserId(@Param("userId") int userId);
}
