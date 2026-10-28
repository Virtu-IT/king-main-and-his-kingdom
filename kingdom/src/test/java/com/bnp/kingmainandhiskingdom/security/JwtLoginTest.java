package com.bnp.kingmainandhiskingdom.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Day 2: log in once, get a JWT, send it as a Bearer token. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtLoginTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asString();
    }

    @Test
    void loginGivesATokenThatOpensProtectedEndpoints() throws Exception {
        String token = login("king", "king1234");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("king"));
        mockMvc.perform(get("/api/persons").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void theRolesInTheTokenDecideWhatIsAllowed() throws Exception {
        String kingToken = login("king", "king1234");
        String subjectToken = login("peasant", "peasant1234");

        mockMvc.perform(delete("/api/persons/Nobody").header("Authorization", "Bearer " + subjectToken))
                .andExpect(status().isForbidden());
        // the king passes the role check; the person does not exist, so we get a 404 instead of a 403
        mockMvc.perform(delete("/api/persons/Nobody").header("Authorization", "Bearer " + kingToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void wrongPasswordCannotLogIn() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"king\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUserCannotLogIn() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"whatever1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anExpiredTokenIsRejected() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("king").issuedAt(past.minusSeconds(60)).expiresAt(past)
                .claim(JwtService.ROLES_CLAIM, List.of("KING"))
                .build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/api/persons").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerCreatesASubjectWhoCanLogIn() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newcomer\",\"password\":\"newcomer123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("SUBJECT"));

        String token = login("newcomer", "newcomer123");
        mockMvc.perform(delete("/api/persons/Arthur").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerRejectsDuplicatesAndWeakPasswords() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"KING\",\"password\":\"longenough1\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"shorty\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }
}
