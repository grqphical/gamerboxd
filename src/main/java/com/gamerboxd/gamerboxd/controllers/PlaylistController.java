package com.gamerboxd.gamerboxd.controllers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Playlist;
import com.gamerboxd.gamerboxd.models.PlaylistItem;
import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.NotificationService;
import com.gamerboxd.gamerboxd.services.PlaylistService;

@Controller
public class PlaylistController {
    private final PlaylistService playlistService;
    private final GameService gameService;
    private final NotificationService notificationService;

    public PlaylistController(PlaylistService playlistService, GameService gameService,
            NotificationService notificationService) {
        this.playlistService = playlistService;
        this.gameService = gameService;
        this.notificationService = notificationService;
    }

    @GetMapping("/playlists")
    public String browse(@AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("playlists", playlistService.getPublic());
        if (currentUser != null)
            model.addAttribute("notificationCount", notificationService.getUserNotificationCount(currentUser.getId()));
        return "playlists/index";
    }

    @PostMapping("/playlists")
    public String create(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam String title,
            @RequestParam(defaultValue = "") String description,
            RedirectAttributes redirect) {
        try {
            return "redirect:/playlists/" + playlistService.create(currentUser.getId(), title, description).getId();
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/playlists";
        }
    }

    @GetMapping("/playlists/{id}")
    public String detail(@PathVariable int id,
            @RequestParam(defaultValue = "") String q,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            Model model) {
        Integer viewerId = currentUser == null ? null : currentUser.getId();
        Playlist playlist = playlistService.getVisible(id, viewerId);
        boolean isOwner = viewerId != null && viewerId == playlist.getUser().getId();
        List<PlaylistItem> items = playlistService.getItems(id);

        // Uncategorised games first, then each category in the order it first appears
        Map<String, List<PlaylistItem>> itemsByCategory = new LinkedHashMap<>();
        itemsByCategory.put("", new ArrayList<>());
        for (PlaylistItem item : items) {
            String category = item.getCategory() == null ? "" : item.getCategory();
            itemsByCategory.computeIfAbsent(category, c -> new ArrayList<>()).add(item);
        }
        if (itemsByCategory.get("").isEmpty()) {
            itemsByCategory.remove("");
        }

        String query = q.trim();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("playlist", playlist);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("itemCount", items.size());
        model.addAttribute("itemsByCategory", itemsByCategory);
        model.addAttribute("games", gameService.getGamesByIDs(items.stream().map(PlaylistItem::getGameId).toList()));
        model.addAttribute("likeCount", playlistService.getLikeCount(id));
        model.addAttribute("liked", viewerId != null && playlistService.isLikedBy(id, viewerId));
        model.addAttribute("query", query);
        model.addAttribute("gameResults", isOwner && !query.isEmpty()
                ? gameService.getSearchResultsByName(query, 10)
                : List.of());
        if (currentUser != null)
            model.addAttribute("notificationCount", notificationService.getUserNotificationCount(currentUser.getId()));
        return "playlists/detail";
    }

    @PostMapping("/playlists/{id}")
    public String update(@PathVariable int id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam String title,
            @RequestParam(defaultValue = "") String description,
            RedirectAttributes redirect) {
        return backToPlaylist(id, redirect,
                () -> playlistService.update(id, currentUser.getId(), title, description));
    }

    @PostMapping("/playlists/{id}/delete")
    public String delete(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        playlistService.delete(id, currentUser.getId());
        return "redirect:/users/" + currentUser.getId();
    }

    @PostMapping("/playlists/{id}/like")
    public String like(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        playlistService.toggleLike(id, currentUser.getId());
        return "redirect:/playlists/" + id;
    }

    @PostMapping("/playlists/{id}/items")
    public String addGame(@PathVariable int id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam int gameId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer rating,
            RedirectAttributes redirect) {
        return backToPlaylist(id, redirect,
                () -> playlistService.addGame(id, currentUser.getId(), gameId, category, rating));
    }

    @PostMapping("/playlists/{id}/items/{itemId}")
    public String updateItem(@PathVariable int id,
            @PathVariable int itemId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer rating,
            RedirectAttributes redirect) {
        return backToPlaylist(id, redirect,
                () -> playlistService.updateItem(id, currentUser.getId(), itemId, category, rating));
    }

    @PostMapping("/playlists/{id}/items/{itemId}/delete")
    public String removeItem(@PathVariable int id,
            @PathVariable int itemId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        playlistService.removeItem(id, currentUser.getId(), itemId);
        return "redirect:/playlists/" + id;
    }

    /**
     * Runs an edit and returns to the playlist, showing the message if the input
     * was invalid.
     */
    private String backToPlaylist(int id, RedirectAttributes redirect, Runnable edit) {
        try {
            edit.run();
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/playlists/" + id;
    }
}
