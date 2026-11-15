package com.gamerboxd.gamerboxd.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.models.GameStatus;

@Service
public class SteamLibraryLinkJobService {
    private final UserService userService;
    private final GameService gameService;

    public SteamLibraryLinkJobService(UserService userService, GameService gameService) {
        this.userService = userService;
        this.gameService = gameService;
    }

    @Async
    public void linkUserSteamLibrary(int userId, List<Integer> selectedAppIds, Map<String, String> params) {
        List<Integer> gameIds = new ArrayList<>();

        for (int appId : selectedAppIds) {
            String name = params.get("gameNames[" + appId + "]");
            Optional<Game> game = gameService.getGameBySteamAppID(appId, name);
            game.ifPresent(g -> gameIds.add(g.getId()));

            // make sure we don't spam the IGDB API
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // still save what we collected so far
            }
        }

        userService.setGameStatuses(userId, gameIds, GameStatus.PLAYED);
    }
}
