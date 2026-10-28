package com.bnp.kingmainandhiskingdom.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Course material: 401 vs 403, public vs protected URLs, roles, HTTP Basic, CORS, password hashes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppUserRepository users;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---- 401: not logged in ----------------------------------------------------------------

    @Test
    void publicEndpointNeedsNoLogin() throws Exception {
        mockMvc.perform(get("/api/welcome").param("name", "Arthur"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutLoginIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"kingdom\""));
    }

    @Test
    void ajaxCallsDoNotGetTheBrowserLoginPopup() throws Exception {
        mockMvc.perform(get("/api/persons").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/persons").with(httpBasic("king", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bogusBearerTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/persons").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    // ---- 403: logged in, but wrong role ------------------------------------------------------

    @Test
    void httpBasicWorksForAnyLoggedInUser() throws Exception {
        mockMvc.perform(get("/api/persons").with(httpBasic("peasant", "peasant1234")))
                .andExpect(status().isOk());
    }

    @Test
    void aSubjectCannotDeletePersons() throws Exception {
        mockMvc.perform(delete("/api/persons/Arthur").with(httpBasic("peasant", "peasant1234")))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyKingsAndKnightsMayTrainPersons() throws Exception {
        String body = "{\"profession\": \"FARMER\"}";
        mockMvc.perform(post("/api/persons/Guinevere/training").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(httpBasic("peasant", "peasant1234")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/persons/Guinevere/training").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(httpBasic("lancelot", "lancelot1234")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "KNIGHT")
    void aKnightCannotDeletePersonsEither() throws Exception {
        mockMvc.perform(delete("/api/persons/Arthur"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUBJECT")
    void aSubjectCannotAddEquipmentButAKnightCan() throws Exception {
        String sword = "{\"type\":\"WEAPON\",\"name\":\"Sword\",\"weight\":1.2,\"damage\":50,\"damageType\":\"SLASHING\"}";
        mockMvc.perform(post("/api/equipment").contentType(MediaType.APPLICATION_JSON).content(sword))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/equipment").contentType(MediaType.APPLICATION_JSON).content(sword)
                        .with(httpBasic("lancelot", "lancelot1234")))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "KNIGHT")
    void actuatorIsForTheKingOnly() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "KING")
    void theKingPassesTheActuatorRuleButTheEndpointIsNotExposed() throws Exception {
        // allowed by the URL rule, but only "health" is exposed: so 404 instead of 403
        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }

    // ---- frontend and documentation stay reachable --------------------------------------------

    @Test
    void angularRoutesAndDocumentationNeedNoLogin() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        // an unknown, non-API URL is not blocked by security (the frontend router handles it)
        int statusCode = mockMvc.perform(get("/persons")).andReturn().getResponse().getStatus();
        assertNotEquals(401, statusCode);
        assertNotEquals(403, statusCode);
    }

    // ---- CORS -------------------------------------------------------------------------------

    @Test
    void corsPreflightFromTheAngularDevServerIsAllowed() throws Exception {
        mockMvc.perform(options("/api/persons")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void corsPreflightFromAnotherSiteIsRejected() throws Exception {
        mockMvc.perform(options("/api/persons")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    // ---- passwords ---------------------------------------------------------------------------

    @Test
    void passwordsAreStoredAsBCryptHashes() {
        AppUser king = users.findByUsernameIgnoreCase("KING").orElseThrow();
        assertNotEquals("king1234", king.getPasswordHash());
        assertTrue(king.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches("king1234", king.getPasswordHash()));
    }

    @Test
    void meEndpointShowsTheLoggedInUser() throws Exception {
        mockMvc.perform(get("/api/auth/me").with(httpBasic("king", "king1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("king"))
                .andExpect(jsonPath("$.roles[0]").value("KING"))
                .andExpect(jsonPath("$.roles[1]").value("KNIGHT"));
    }
}
