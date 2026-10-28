package com.bnp.kingmainandhiskingdom.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The one public endpoint of the API (see SecurityConfig): anybody may be welcomed. */
@RestController
@RequestMapping("/api")
public class WelcomeRestController {

    @GetMapping("/welcome")
    public String welcome(@RequestParam(required = false) String name) {
        return "Welcome to the Kingdom, " + (name == null ? "Guest" : name) + "!";
    }
}
