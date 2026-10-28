package com.bnp.kingmainandhiskingdom.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Exercise B5: the audit log says who did what, but never contains a password or a token. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AuditLogTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aRefusedMethodCallIsLoggedWithWhoAndWhat(CapturedOutput output) throws Exception {
        mockMvc.perform(delete("/api/persons/Arthur").with(httpBasic("peasant", "peasant1234")));

        assertTrue(output.getAll().contains("AUDIT access denied user=peasant action=PersonRestController.deletePerson"),
                "audit line missing in: " + output.getAll());
    }

    @Test
    void aRefusedUrlIsLoggedToo(CapturedOutput output) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/stats")
                .with(httpBasic("peasant", "peasant1234")));

        assertTrue(output.getAll().contains("AUDIT access denied user=peasant action=GET /api/stats"));
    }

    @Test
    void loginsAreLoggedWithoutPasswordsOrTokens(CapturedOutput output) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"lancelot\",\"password\":\"lancelot1234\"}"))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"lancelot\",\"password\":\"guess-wrong-1\"}"));

        String log = output.getAll();
        assertTrue(log.contains("AUDIT login success user=lancelot"));
        assertTrue(log.contains("AUDIT login failure user=lancelot reason=BadCredentialsException"));
        assertFalse(log.contains("lancelot1234"), "the password must never be logged");
        assertFalse(log.contains("guess-wrong-1"), "a wrong password must never be logged either");
        String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        assertFalse(log.contains(token), "a token must never be logged");
    }
}
