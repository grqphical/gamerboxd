package com.gamerboxd.gamerboxd.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class UserController {
    @GetMapping("/users/{id}")
    public String getUserProfile(@PathVariable Long id) {
        return "users/profile";
    }
}
