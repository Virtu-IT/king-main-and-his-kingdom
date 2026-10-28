package com.bnp.kingmainandhiskingdom.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercise A7: a user with an old {bcrypt} hash can log in, and the hash is replaced by a {pbkdf2} hash. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordUpgradeTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppUserRepository users;

    @Test
    void anOldBcryptHashIsUpgradedAfterALogin() throws Exception {
        String oldHash = "{bcrypt}" + new BCryptPasswordEncoder().encode("legacy1234");
        users.save(new AppUser("legacy", oldHash, Set.of("SUBJECT")));

        mockMvc.perform(get("/api/auth/me").with(httpBasic("legacy", "legacy1234"))).andExpect(status().isOk());

        String newHash = users.findByUsernameIgnoreCase("legacy").orElseThrow().getPasswordHash();
        assertTrue(newHash.startsWith("{pbkdf2}"), "the hash was upgraded: " + newHash.substring(0, 10));

        // the new hash works and the old password is still the password
        mockMvc.perform(get("/api/auth/me").with(httpBasic("legacy", "legacy1234"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").with(httpBasic("legacy", "wrong-one"))).andExpect(status().isUnauthorized());
    }

    @Test
    void aWrongPasswordNeverUpgradesTheHash() throws Exception {
        String oldHash = "{bcrypt}" + new BCryptPasswordEncoder().encode("legacy5678");
        users.save(new AppUser("legacy2", oldHash, Set.of("SUBJECT")));

        mockMvc.perform(get("/api/auth/me").with(httpBasic("legacy2", "wrong-one"))).andExpect(status().isUnauthorized());

        assertEquals(oldHash, users.findByUsernameIgnoreCase("legacy2").orElseThrow().getPasswordHash());
    }
}
