package com.bnp.kingmainandhiskingdom.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercise B6: sessions and CSRF on /legacy/**, next to the stateless API that has no CSRF protection. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegacyCsrfTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anAnonymousVisitorIsSentToTheLoginPage() throws Exception {
        mockMvc.perform(get("/legacy/hello"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/legacy/login"));
    }

    @Test
    void theLoginPageContainsACsrfToken() throws Exception {
        mockMvc.perform(get("/legacy/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name='_csrf'")));
    }

    @Test
    void aLoginWithoutCsrfTokenIsRefused_withTheTokenItWorksAndCreatesASession() throws Exception {
        mockMvc.perform(post("/legacy/login").param("username", "king").param("password", "king1234"))
                .andExpect(status().isForbidden());

        // formLogin() of Spring Security Test adds the CSRF token for us
        MvcResult result = mockMvc.perform(formLogin("/legacy/login").user("king").password("king1234"))
                .andExpect(authenticated().withUsername("king"))
                .andExpect(redirectedUrl("/legacy/hello"))
                .andReturn();
        // a session: the server remembers the login (the JWT API remembers nothing)
        assertNotNull(result.getRequest().getSession(false));
    }

    @Test
    void aPostWithoutCsrfTokenIsRefusedEvenWhenLoggedIn() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(formLogin("/legacy/login").user("king").password("king1234"))
                .andReturn().getRequest().getSession();

        mockMvc.perform(post("/legacy/note").session(session).param("text", "forged"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/legacy/note").session(session).with(csrf()).param("text", "hello <b>world</b>"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/legacy/hello"));

        // and the note is escaped when it is shown again
        mockMvc.perform(get("/legacy/hello").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hello &lt;b&gt;world&lt;/b&gt;")));
    }

    @Test
    void theJwtApiNeedsNoCsrfTokenBecauseItHasNoCookie() throws Exception {
        // a POST to the API with HTTP Basic and without any CSRF token: fine
        int status = mockMvc.perform(post("/api/persons").with(httpBasic("king", "king1234"))
                        .contentType("application/json").content("{\"name\":\"Csrf-free\",\"age\":30}"))
                .andReturn().getResponse().getStatus();
        assertTrue(status == 201, "expected 201 but was " + status);
    }

    @Test
    void theLegacyChainDoesNotAcceptTheApiLoginMethods() throws Exception {
        // HTTP Basic is not enabled on this chain: the user from httpBasic is ignored and we are sent to the login page
        mockMvc.perform(get("/legacy/hello").with(httpBasic("king", "king1234")))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/legacy/hello").with(user("someone").roles("SUBJECT")))
                .andExpect(status().isOk());
    }
}
