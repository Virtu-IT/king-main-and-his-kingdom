package com.bnp.kingmainandhiskingdom.controller;

import com.bnp.kingmainandhiskingdom.repository.EquipmentRepository;
import com.bnp.kingmainandhiskingdom.repository.PersonRepository;
import com.bnp.kingmainandhiskingdom.repository.WarMachineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercise A2: statistics for the king and the knights only. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatsRestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PersonRepository persons;
    @Autowired
    private EquipmentRepository equipment;
    @Autowired
    private WarMachineRepository warMachines;

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    void aSubjectIsForbidden() throws Exception {
        mockMvc.perform(get("/api/stats").with(user("peasant").roles("SUBJECT"))).andExpect(status().isForbidden());
    }

    @Test
    void aKnightSeesTheNumbers() throws Exception {
        mockMvc.perform(get("/api/stats").with(user("lancelot").roles("KNIGHT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persons").value(persons.count()))
                .andExpect(jsonPath("$.equipment").value(equipment.count()))
                .andExpect(jsonPath("$.warMachines").value(warMachines.count()));
    }

    @Test
    void theKingSeesTheNumbers() throws Exception {
        mockMvc.perform(get("/api/stats").with(user("king").roles("KING"))).andExpect(status().isOk());
    }
}
