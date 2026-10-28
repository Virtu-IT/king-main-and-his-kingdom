package com.bnp.kingmainandhiskingdom.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercise B1: API key for an integration. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiKeyTest {

    @Autowired
    private MockMvc mockMvc;
    @Value("${kingdom.security.api-key}")
    private String apiKey;

    @Test
    void theRightKeyGivesAccess() throws Exception {
        mockMvc.perform(get("/api/integration/report").header("api-key", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persons").isNumber());
    }

    @Test
    void noKeyIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/integration/report")).andExpect(status().isUnauthorized());
    }

    @Test
    void aWrongKeyIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/integration/report").header("api-key", apiKey + "x"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/integration/report").header("api-key", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aNormalUserWithoutTheIntegrationRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/api/integration/report").with(httpBasic("king", "king1234")))
                .andExpect(status().isForbidden());
    }

    @Test
    void theKeyOnlyWorksOnTheIntegrationEndpoints() throws Exception {
        mockMvc.perform(get("/api/persons").header("api-key", apiKey)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/stats").header("api-key", apiKey)).andExpect(status().isUnauthorized());
    }
}
