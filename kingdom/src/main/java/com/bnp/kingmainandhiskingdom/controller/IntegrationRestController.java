package com.bnp.kingmainandhiskingdom.controller;

import com.bnp.kingmainandhiskingdom.services.KingdomStatsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exercise B1: for other systems, authenticated with an API key (header "api-key"), not with a user login. */
@RestController
@RequestMapping(path = "/api/integration", produces = MediaType.APPLICATION_JSON_VALUE)
public class IntegrationRestController {

    private final KingdomStatsService statsService;

    public IntegrationRestController(KingdomStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/report")
    public Report report() {
        return new Report(statsService.stats().persons());
    }

    public record Report(long persons) {
    }
}
