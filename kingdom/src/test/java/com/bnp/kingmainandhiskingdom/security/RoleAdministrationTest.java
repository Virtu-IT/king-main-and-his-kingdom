package com.bnp.kingmainandhiskingdom.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercise B2: the king manages the roles. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleAdministrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountService accounts;

    @Test
    void theKingPromotesAPeasantToKnight() throws Exception {
        accounts.create("promotee", "promotee1234", Set.of("SUBJECT"));

        mockMvc.perform(put("/api/admin/users/promotee/roles").with(httpBasic("king", "king1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"KNIGHT\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("KNIGHT"));

        // the next login (HTTP Basic reads the database on every request) shows the new role
        mockMvc.perform(get("/api/auth/me").with(httpBasic("promotee", "promotee1234")))
                .andExpect(jsonPath("$.roles[0]").value("KNIGHT"));
    }

    @Test
    void aKnightCannotChangeRoles() throws Exception {
        mockMvc.perform(put("/api/admin/users/peasant/roles").with(httpBasic("lancelot", "lancelot1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"KING\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownRolesAreRejected() throws Exception {
        mockMvc.perform(put("/api/admin/users/peasant/roles").with(httpBasic("king", "king1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"EMPEROR\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anEmptyListOfRolesIsRejected() throws Exception {
        mockMvc.perform(put("/api/admin/users/peasant/roles").with(httpBasic("king", "king1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theKingCannotRemoveTheirOwnKingRole() throws Exception {
        mockMvc.perform(put("/api/admin/users/king/roles").with(httpBasic("king", "king1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"KNIGHT\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownUserIsNotFound() throws Exception {
        mockMvc.perform(put("/api/admin/users/nobody/roles").with(httpBasic("king", "king1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"KNIGHT\"]}"))
                .andExpect(status().isNotFound());
    }
}
