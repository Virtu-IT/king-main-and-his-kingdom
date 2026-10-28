package com.bnp.kingmainandhiskingdom.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercise C3: the buildings. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BuildingRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void everybodyLoggedInSeesTheBuildingsWithTheirOwnFields() throws Exception {
        mockMvc.perform(get("/api/buildings").with(user("peasant").roles("SUBJECT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Camelot')].type").value("CASTLE"))
                .andExpect(jsonPath("$[?(@.name == 'Camelot')].towers").value(7))
                .andExpect(jsonPath("$[?(@.name == 'Avalon Farm')].crop").value("wheat"))
                .andExpect(jsonPath("$[?(@.name == 'Watchtower North')].crewSize").value(4));
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/buildings")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyTheKingBuilds() throws Exception {
        String farm = "{\"type\":\"FARM\",\"name\":\"New farm\",\"hectares\":12,\"crop\":\"rye\"}";
        mockMvc.perform(post("/api/buildings").with(user("lancelot").roles("KNIGHT"))
                        .contentType(MediaType.APPLICATION_JSON).content(farm))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/buildings").with(user("king").roles("KING"))
                        .contentType(MediaType.APPLICATION_JSON).content(farm))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("FARM"))
                .andExpect(jsonPath("$.hectares").value(12));
    }

    @Test
    void theRulesOfTheSubclassesAreValidatedInTheDto() throws Exception {
        // the database allows NULL in these columns (SINGLE_TABLE), so the API has to insist
        mockMvc.perform(post("/api/buildings").with(user("king").roles("KING"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CASTLE\",\"name\":\"Towerless\",\"hasMoat\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Error-Message", "towers: Towers is required"));
        mockMvc.perform(post("/api/buildings").with(user("king").roles("KING"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TOWER\",\"name\":\"Underground\",\"heightMeters\":-5,\"crewSize\":2}"))
                .andExpect(status().isBadRequest());
    }
}
