package com.bnp.kingmainandhiskingdom.controller;

import com.bnp.kingmainandhiskingdom.services.KingdomStatsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exercise A2. The access rule (KING and KNIGHT) is a URL rule in SecurityConfig. The alternative is
 * a @PreAuthorize("hasAnyRole('KING', 'KNIGHT')") on the method below; see SOLUTIONS.md for when to choose which.
 */
@RestController
@RequestMapping(path = "/api/stats", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatsRestController {

    private final KingdomStatsService statsService;

    public StatsRestController(KingdomStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public KingdomStatsService.Stats stats() {
        return statsService.stats();
    }
}
