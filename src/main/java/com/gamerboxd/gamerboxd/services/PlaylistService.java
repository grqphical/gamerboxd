package com.gamerboxd.gamerboxd.services;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.Playlist;
import com.gamerboxd.gamerboxd.models.PlaylistItem;
import com.gamerboxd.gamerboxd.models.PlaylistLike;
import com.gamerboxd.gamerboxd.models.PlaylistSummary;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.repositories.PlaylistItemRepository;
import com.gamerboxd.gamerboxd.repositories.PlaylistLikeRepository;
import com.gamerboxd.gamerboxd.repositories.PlaylistRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@Service
public class PlaylistService {
    public static final int MAX_GAMES = 200;

    private final PlaylistRepository playlistRepository;
    private final PlaylistItemRepository itemRepository;
    private final PlaylistLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final GameService gameService;

    public PlaylistService(PlaylistRepository playlistRepository, PlaylistItemRepository itemRepository,
            PlaylistLikeRepository likeRepository, UserRepository userRepository, GameService gameService) {
        this.playlistRepository = playlistRepository;
        this.itemRepository = itemRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.gameService = gameService;
    }

    public List<PlaylistSummary> getPublic() {
        return playlistRepository.findPublicSummaries();
    }

    public List<PlaylistSummary> getAllByUserId(int userId) {
        return playlistRepository.findSummariesByUserId(userId);
    }

    public Playlist create(int userId, String title, String description) {
        Playlist playlist = new Playlist(userRepository.getReferenceById(userId), null, null);
        applyDetails(playlist, title, description);
        return playlistRepository.save(playlist);
    }

    /** Playlists of private accounts are only visible to their owner. */
    public Playlist getVisible(int playlistId, Integer viewerId) {
        Playlist playlist = playlistRepository.findById(playlistId).orElseThrow(PlaylistService::notFound);
        User owner = playlist.getUser();
        if (owner.isPrivateAccount() && (viewerId == null || viewerId != owner.getId())) {
            throw notFound();
        }
        return playlist;
    }

    public Playlist getOwned(int playlistId, int userId) {
        Playlist playlist = playlistRepository.findById(playlistId).orElseThrow(PlaylistService::notFound);
        if (playlist.getUser().getId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own playlists.");
        }
        return playlist;
    }

    public void update(int playlistId, int userId, String title, String description) {
        Playlist playlist = getOwned(playlistId, userId);
        applyDetails(playlist, title, description);
        playlistRepository.save(playlist);
    }

    /** Items and likes go with it through the database's cascading deletes. */
    public void delete(int playlistId, int userId) {
        playlistRepository.delete(getOwned(playlistId, userId));
    }

    public List<PlaylistItem> getItems(int playlistId) {
        return itemRepository.findAllByPlaylistIdOrderById(playlistId);
    }

    public PlaylistItem addGame(int playlistId, int userId, int gameId, String category, Integer rating) {
        Playlist playlist = getOwned(playlistId, userId);
        validateRating(rating);
        if (itemRepository.existsByPlaylistIdAndGameId(playlistId, gameId)) {
            throw new IllegalArgumentException("That game is already in this playlist.");
        }
        if (getItems(playlistId).size() >= MAX_GAMES) {
            throw new IllegalArgumentException("A playlist can hold at most " + MAX_GAMES + " games.");
        }
        if (gameService.getGameByID(gameId).isEmpty()) {
            throw new IllegalArgumentException("Invalid Game ID");
        }
        return itemRepository.save(new PlaylistItem(playlist, gameId, cleanCategory(category), rating));
    }

    public void updateItem(int playlistId, int userId, int itemId, String category, Integer rating) {
        PlaylistItem item = getOwnedItem(playlistId, userId, itemId);
        validateRating(rating);
        item.setCategory(cleanCategory(category));
        item.setRating(rating);
        itemRepository.save(item);
    }

    public void removeItem(int playlistId, int userId, int itemId) {
        itemRepository.delete(getOwnedItem(playlistId, userId, itemId));
    }

    /** Likes the playlist if not liked yet, otherwise removes the like. Returns true if now liked. */
    public boolean toggleLike(int playlistId, int userId) {
        Playlist playlist = getVisible(playlistId, userId);
        Optional<PlaylistLike> existing = likeRepository.findByPlaylistIdAndUserId(playlistId, userId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            return false;
        }
        likeRepository.save(new PlaylistLike(playlist, userRepository.getReferenceById(userId)));
        return true;
    }

    public long getLikeCount(int playlistId) {
        return likeRepository.countByPlaylistId(playlistId);
    }

    public boolean isLikedBy(int playlistId, int userId) {
        return likeRepository.findByPlaylistIdAndUserId(playlistId, userId).isPresent();
    }

    private PlaylistItem getOwnedItem(int playlistId, int userId, int itemId) {
        getOwned(playlistId, userId);
        return itemRepository.findById(itemId)
                .filter(item -> item.getPlaylist().getId() == playlistId)
                .orElseThrow(PlaylistService::notFound);
    }

    private void applyDetails(Playlist playlist, String title, String description) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Playlist title is required.");
        }
        if (title.trim().length() > 100) {
            throw new IllegalArgumentException("Playlist title must be 100 characters or fewer.");
        }
        String cleanDescription = description == null ? "" : description.trim();
        if (cleanDescription.length() > 1000) {
            throw new IllegalArgumentException("Playlist description must be 1000 characters or fewer.");
        }
        playlist.setTitle(title.trim());
        playlist.setDescription(cleanDescription);
    }

    private static void validateRating(Integer rating) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
    }

    private static String cleanCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        if (category.trim().length() > 50) {
            throw new IllegalArgumentException("Category must be 50 characters or fewer.");
        }
        return category.trim();
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found.");
    }
}
