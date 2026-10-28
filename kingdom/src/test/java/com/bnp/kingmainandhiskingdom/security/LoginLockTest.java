package com.bnp.kingmainandhiskingdom.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.bnp.kingmainandhiskingdom.support.MutableClock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercise B3: three failed logins block the username for a minute (the clock is moved forward by the test). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginLockTest {

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
    private AccountService accounts;
    @Autowired
    private MutableClock clock;

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void threeWrongPasswordsLockTheAccount_evenForTheCorrectPassword_untilTheTimeHasPassed() throws Exception {
        accounts.create("locker", "locker1234", Set.of("SUBJECT"));

        for (int i = 0; i < 3; i++) {
            login("locker", "wrong-" + i).andExpect(status().isUnauthorized());
        }
        // the 4th attempt has the CORRECT password and is refused anyway
        login("locker", "locker1234")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
        // HTTP Basic cannot be used to get around the lock
        mockMvc.perform(get("/api/auth/me").with(httpBasic("locker", "locker1234"))).andExpect(status().isUnauthorized());

        clock.advance(Duration.ofSeconds(30));
        login("locker", "locker1234").andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "30"));

        clock.advance(Duration.ofSeconds(31));
        login("locker", "locker1234").andExpect(status().isOk());
    }

    @Test
    void anUnknownUserIsBlockedInExactlyTheSameWay() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("ghost", "whatever-" + i).andExpect(status().isUnauthorized());
        }
        login("ghost", "whatever-again").andExpect(status().isTooManyRequests());
    }

    @Test
    void aSuccessfulLoginResetsTheCounter() throws Exception {
        accounts.create("resetter", "resetter1234", Set.of("SUBJECT"));

        login("resetter", "wrong-1").andExpect(status().isUnauthorized());
        login("resetter", "wrong-2").andExpect(status().isUnauthorized());
        login("resetter", "resetter1234").andExpect(status().isOk());
        login("resetter", "wrong-3").andExpect(status().isUnauthorized());
        login("resetter", "wrong-4").andExpect(status().isUnauthorized());
        // two failures since the last success: not locked
        login("resetter", "resetter1234").andExpect(status().isOk());
    }
}
