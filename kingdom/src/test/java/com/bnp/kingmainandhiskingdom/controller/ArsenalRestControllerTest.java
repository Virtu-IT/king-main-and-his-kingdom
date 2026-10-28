package com.bnp.kingmainandhiskingdom.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "KING")
class ArsenalRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void equipmentListContainsWeaponsAndArmorWithTheirOwnFields() throws Exception {
        mockMvc.perform(get("/api/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Excalibur')].type").value("WEAPON"))
                .andExpect(jsonPath("$[?(@.name == 'Excalibur')].damage").value(95))
                .andExpect(jsonPath("$[?(@.name == 'Plate armor')].type").value("ARMOR"))
                .andExpect(jsonPath("$[?(@.name == 'Plate armor')].defense").value(80));
    }

    @Test
    void addArmor() throws Exception {
        mockMvc.perform(post("/api/equipment").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ARMOR\",\"name\":\"Helmet\",\"weight\":2.0,\"defense\":30,\"material\":\"Iron\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ARMOR"))
                .andExpect(jsonPath("$.material").value("Iron"));
    }

    @Test
    void warMachinesCanBeSearched() throws Exception {
        mockMvc.perform(get("/api/war-machines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Mark I')].type").value("TANK"))
                .andExpect(jsonPath("$[?(@.name == 'Trebuchet')].rangeMeters").value(300));
        mockMvc.perform(get("/api/war-machines").param("name", "treb").param("minCrew", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void addWarMachine() throws Exception {
        mockMvc.perform(post("/api/war-machines").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CATAPULT\",\"name\":\"Scorpion\",\"crewSize\":2,\"rangeMeters\":90}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CATAPULT"));
    }
}
