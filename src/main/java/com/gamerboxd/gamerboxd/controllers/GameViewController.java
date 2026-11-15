package com.gamerboxd.gamerboxd.controllers;

import java.util.Optional;
import java.util.Set;
import java.lang.classfile.ClassFile.Option;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.services.ReviewService;

import com.gamerboxd.gamerboxd.services.GameService;
import com.gamerboxd.gamerboxd.services.ReviewLikeService;
import com.gamerboxd.gamerboxd.models.Game;
import com.gamerboxd.gamerboxd.models.Review;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;


@Controller
public class GameViewController {
    private final GameService gameService;

    public GameViewController(GameService gameService) {
       this.gameService = gameService;
    }

    @GetMapping("/games/{id}")
    public String gameView(@PathVariable int id, Model model) {
        Game game = gameService.getGameByID(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game ID does not match an existing game."));
        model.addAttribute("game", game);
        return "games";
    }
}