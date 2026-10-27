package com.gamerboxd.gamerboxd.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GamerboxdController {
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
