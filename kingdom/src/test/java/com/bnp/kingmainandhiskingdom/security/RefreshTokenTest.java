package com.bnp.kingmainandhiskingdom.security;

import com.bnp.kingmainandhiskingdom.support.MutableClock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercise B4: refresh tokens with rotation. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshTokenTest {

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccountService accounts;
    @Autowired
    private RefreshTokenRepository refreshTokens;
    @Autowired
    private MutableClock clock;

    private JsonNode json(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode login(String username, String password) throws Exception {
        return json(postJson("/api/auth/login", "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}").andExpect(status().isOk()));
    }

    @Test
    void loginGivesARefreshTokenAndItIsOnlyStoredAsAHash() throws Exception {
        accounts.create("hasher", "hasher1234", Set.of("SUBJECT"));
        String refreshToken = login("hasher", "hasher1234").get("refreshToken").asString();

        assertTrue(refreshToken.length() >= 43, "a random 256-bit token");
        assertTrue(refreshTokens.findAll().stream().noneMatch(t -> t.getTokenHash().equals(refreshToken)),
                "the token itself is not stored");
        assertTrue(refreshTokens.findByTokenHash(RefreshTokenService.hash(refreshToken)).isPresent());
    }

    @Test
    void refreshingGivesNewTokensAndTheOldRefreshTokenStopsWorking() throws Exception {
        accounts.create("rotator", "rotator1234", Set.of("SUBJECT"));
        JsonNode first = login("rotator", "rotator1234");
        String oldRefresh = first.get("refreshToken").asString();

        JsonNode second = json(postJson("/api/auth/refresh", "{\"refreshToken\":\"" + oldRefresh + "\"}").andExpect(status().isOk()));
        String newRefresh = second.get("refreshToken").asString();
        assertNotEquals(oldRefresh, newRefresh);

        // the new access token works
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + second.get("token").asString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("rotator"));

        // reusing the OLD refresh token is refused
        postJson("/api/auth/refresh", "{\"refreshToken\":\"" + oldRefresh + "\"}").andExpect(status().isUnauthorized());
        // the new one still works once
        postJson("/api/auth/refresh", "{\"refreshToken\":\"" + newRefresh + "\"}").andExpect(status().isOk());
    }

    @Test
    void aRefreshReadsTheRolesFromTheDatabaseAgain() throws Exception {
        accounts.create("promoted", "promoted1234", Set.of("SUBJECT"));
        JsonNode first = login("promoted", "promoted1234");
        assertEquals("SUBJECT", first.get("roles").get(0).asString());

        accounts.changeRoles("king", "promoted", Set.of("KNIGHT"));

        JsonNode refreshed = json(postJson("/api/auth/refresh", "{\"refreshToken\":\"" + first.get("refreshToken").asString() + "\"}")
                .andExpect(status().isOk()));
        assertEquals("KNIGHT", refreshed.get("roles").get(0).asString());
    }

    @Test
    void anUnknownRefreshTokenIsRefused() throws Exception {
        postJson("/api/auth/refresh", "{\"refreshToken\":\"not-a-real-token\"}").andExpect(status().isUnauthorized());
        postJson("/api/auth/refresh", "{}").andExpect(status().isBadRequest());
    }

    @Test
    void anExpiredRefreshTokenIsRefused() throws Exception {
        accounts.create("slowpoke", "slowpoke1234", Set.of("SUBJECT"));
        String refresh = login("slowpoke", "slowpoke1234").get("refreshToken").asString();

        clock.advance(Duration.ofHours(9)); // refresh-ttl is 8 hours

        postJson("/api/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}").andExpect(status().isUnauthorized());
        assertFalse(refreshTokens.findByTokenHash(RefreshTokenService.hash(refresh)).isPresent(), "the expired token is removed");
    }

    @Test
    void aPasswordChangeRevokesAllRefreshTokens() throws Exception {
        accounts.create("pwchanger", "pwchanger1234", Set.of("SUBJECT"));
        String refresh = login("pwchanger", "pwchanger1234").get("refreshToken").asString();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/users/pwchanger/password")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("pwchanger", "pwchanger1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"pwchanger1234\",\"newPassword\":\"another-password-9\"}"))
                .andExpect(status().isNoContent());

        postJson("/api/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}").andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        accounts.create("leaver", "leaver1234", Set.of("SUBJECT"));
        String refresh = login("leaver", "leaver1234").get("refreshToken").asString();

        postJson("/api/auth/logout", "{\"refreshToken\":\"" + refresh + "\"}").andExpect(status().isNoContent());

        postJson("/api/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}").andExpect(status().isUnauthorized());
    }
}
