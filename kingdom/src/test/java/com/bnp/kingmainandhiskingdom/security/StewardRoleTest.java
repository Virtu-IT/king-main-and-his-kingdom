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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercise A3: the steward manages the equipment but may not see the persons. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StewardRoleTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aStewardCannotSeeThePersons() throws Exception {
        mockMvc.perform(get("/api/persons").with(httpBasic("steward", "steward1234")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/persons/Arthur").with(httpBasic("steward", "steward1234")))
                .andExpect(status().isForbidden());
    }

    @Test
    void aStewardCanSeeAndAddEquipment() throws Exception {
        mockMvc.perform(get("/api/equipment").with(httpBasic("steward", "steward1234")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/equipment").with(httpBasic("steward", "steward1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ARMOR\",\"name\":\"Steward cloak\",\"weight\":1.0,\"defense\":5,\"material\":\"Wool\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void aStewardCannotAddWarMachinesOrTrainPersons() throws Exception {
        mockMvc.perform(post("/api/war-machines").with(httpBasic("steward", "steward1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CATAPULT\",\"name\":\"Nope\",\"crewSize\":1,\"rangeMeters\":10}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/persons/Guinevere/training").with(httpBasic("steward", "steward1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profession\":\"FARMER\"}"))
                .andExpect(status().isForbidden());
    }
}
