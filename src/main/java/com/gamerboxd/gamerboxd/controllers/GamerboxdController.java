package com.gamerboxd.gamerboxd.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;

@Controller
public class GamerboxdController {
    @GetMapping("/")
    public String index(@AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        model.addAttribute("currentUser", currentUser);
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
