package com.bnp.kingmainandhiskingdom.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercise A4: ownership checks with SpEL and changing your own password. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountService accounts;

    @Test
    void youMayLookUpYourself() throws Exception {
        mockMvc.perform(get("/api/users/lancelot").with(httpBasic("lancelot", "lancelot1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("KNIGHT"));
    }

    @Test
    void aKnightCannotLookUpAPeasant() throws Exception {
        mockMvc.perform(get("/api/users/peasant").with(httpBasic("lancelot", "lancelot1234")))
                .andExpect(status().isForbidden());
    }

    @Test
    void theKingMayLookUpEverybody() throws Exception {
        mockMvc.perform(get("/api/users/peasant").with(httpBasic("king", "king1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("peasant"));
        mockMvc.perform(get("/api/users/nobody").with(httpBasic("king", "king1234")))
                .andExpect(status().isNotFound());
    }

    @Test
    void afterAPasswordChangeOnlyTheNewPasswordWorks() throws Exception {
        accounts.create("changer", "oldpassword1", java.util.Set.of(AccountService.ROLE_SUBJECT));

        mockMvc.perform(put("/api/users/changer/password").with(httpBasic("changer", "oldpassword1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"oldpassword1\",\"newPassword\":\"newpassword2\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").with(httpBasic("changer", "oldpassword1"))).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").with(httpBasic("changer", "newpassword2"))).andExpect(status().isOk());
    }

    @Test
    void aWrongOldPasswordOrAWeakNewPasswordIsRejected() throws Exception {
        accounts.create("careful", "oldpassword1", java.util.Set.of(AccountService.ROLE_SUBJECT));

        mockMvc.perform(put("/api/users/careful/password").with(httpBasic("careful", "oldpassword1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"not-my-password\",\"newPassword\":\"newpassword2\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/users/careful/password").with(httpBasic("careful", "oldpassword1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"oldpassword1\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nobodyChangesAnotherUsersPassword_notEvenTheKing() throws Exception {
        mockMvc.perform(put("/api/users/peasant/password").with(httpBasic("king", "king1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"peasant1234\",\"newPassword\":\"newpassword2\"}"))
                .andExpect(status().isForbidden());
    }
}
