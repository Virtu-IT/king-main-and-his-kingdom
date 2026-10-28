package com.bnp.kingmainandhiskingdom.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WelcomeRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testWelcomeDefault() throws Exception {
        mockMvc.perform(get("/api/welcome"))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome to the Kingdom, Guest!"));
    }

    @Test
    void testWelcomeWithCustomName() throws Exception {
        mockMvc.perform(get("/api/welcome").param("name", "Arthur"))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome to the Kingdom, Arthur!"));
    }
}
