package com.bnp.kingmainandhiskingdom.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The king may do everything with persons, so these tests run as a (mock) king. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "king", roles = "KING")
class PersonRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAllPersonsContainsPreFilled() throws Exception {
        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.name == 'Arthur')].age").value(35))
                .andExpect(jsonPath("$[?(@.name == 'Arthur')].type").value("KNIGHT"))
                .andExpect(jsonPath("$[?(@.name == 'Merlin')].mana").value(100))
                .andExpect(jsonPath("$[?(@.name == 'Lancelot')].profession").value("SOLDIER"));
    }

    @Test
    void testCreateAndGetPerson() throws Exception {
        String jsonPayload = """
                {
                    "name": "Percival",
                    "age": 25
                }
                """;

        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Percival"))
                .andExpect(jsonPath("$.age").value(25))
                .andExpect(jsonPath("$.type").value("PERSON"));

        mockMvc.perform(get("/api/persons/Percival"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Percival"));

        mockMvc.perform(get("/api/persons/query").param("name", "Percival"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(25));
    }

    @Test
    void testCreatePolymorphicKnight() throws Exception {
        String jsonPayload = """
                {
                    "type": "KNIGHT",
                    "name": "Bedivere",
                    "age": 29,
                    "knightRank": "Marshal",
                    "horseName": "Sleipnir"
                }
                """;

        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("KNIGHT"))
                .andExpect(jsonPath("$.horseName").value("Sleipnir"));
    }

    @Test
    void testCreatePersonWithNegativeAgeThrowsBadRequest() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Timeless\", \"age\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void testCreatedPersonHasNoProfessionEvenIfOneIsSent() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cheater\", \"age\": 30, \"profession\": \"SOLDIER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profession").doesNotExist());
    }

    @Test
    void testTrainAnExistingPerson() throws Exception {
        mockMvc.perform(post("/api/persons/Guinevere/training")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profession\": \"NURSE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Guinevere"))
                .andExpect(jsonPath("$.profession").value("NURSE"));

        mockMvc.perform(get("/api/persons/Guinevere"))
                .andExpect(jsonPath("$.profession").value("NURSE"));
    }

    @Test
    void testTrainingIsRefusedWhenTheAgeIsTooLow() throws Exception {
        mockMvc.perform(post("/api/persons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"YoungKnight\", \"age\": 18}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/persons/YoungKnight/training")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profession\": \"SOLDIER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string("X-Error-Message", "Too young to become a SOLDIER: minimum age is 21"));
    }

    @Test
    void testCreateAPeasant() throws Exception {
        mockMvc.perform(post("/api/persons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"PEASANT\", \"name\": \"Hodge\", \"age\": 33, \"crop\": \"oats\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PEASANT"))
                .andExpect(jsonPath("$.crop").value("oats"));
        mockMvc.perform(get("/api/persons/Hodge")).andExpect(jsonPath("$.crop").value("oats"));
    }

    @Test
    void testAKnightNeedsARankAndAShortHorseName() throws Exception {
        mockMvc.perform(post("/api/persons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"KNIGHT\", \"name\": \"Rankless\", \"age\": 30, \"horseName\": \"Silver\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Error-Message", "knightRank: A knight needs a rank"));

        String longName = "H".repeat(31);
        mockMvc.perform(post("/api/persons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"KNIGHT\", \"name\": \"Proud\", \"age\": 30, \"knightRank\": \"Squire\", \"horseName\": \"" + longName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Error-Message", "horseName: The name of the horse can have at most 30 characters"));
    }

    @Test
    void testAWizardCannotBeTrainedAsASoldier_andASoldierIsReleasedFirst() throws Exception {
        mockMvc.perform(post("/api/persons/Merlin/training").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profession\": \"SOLDIER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Error-Message", "A wizard cannot become a SOLDIER"));

        // Lancelot is a soldier already: 409 Conflict, not 400 (the request is fine, the state does not allow it)
        mockMvc.perform(post("/api/persons/Lancelot/training").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profession\": \"SOLDIER\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/persons/Lancelot/training"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profession").doesNotExist());
        mockMvc.perform(post("/api/persons/Lancelot/training").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profession\": \"SOLDIER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profession").value("SOLDIER"));
    }

    @Test
    void testTrainABlacksmith() throws Exception {
        mockMvc.perform(post("/api/persons/Arthur/training").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profession\": \"BLACKSMITH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profession").value("BLACKSMITH"));
    }

    @Test
    void testTheEquipmentOfAKnight() throws Exception {
        mockMvc.perform(get("/api/persons/Arthur/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Excalibur')].type").value("WEAPON"))
                .andExpect(jsonPath("$[?(@.name == 'Plate armor')].defense").value(80));
        mockMvc.perform(get("/api/persons/Guinevere/equipment"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Error-Message", "Guinevere is not a knight: only knights carry equipment"));
    }

    @Test
    void testAllKnightsWithEquipment() throws Exception {
        mockMvc.perform(get("/api/knights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.knight == 'Lancelot')].equipment.length()").value(2));
    }

    @Test
    void testTrainingAnUnknownPersonIsNotFound() throws Exception {
        mockMvc.perform(post("/api/persons/Nobody/training")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profession\": \"FARMER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testTrainingNeedsAProfession() throws Exception {
        mockMvc.perform(post("/api/persons/Guinevere/training")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreatePersonWithoutNameIsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"age\": 40}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void testCreateDuplicateIsConflict() throws Exception {
        mockMvc.perform(post("/api/persons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lancelot\", \"age\": 40}"))
                .andExpect(status().isConflict());
    }

    @Test
    void testGetNonExistentPersonThrowsNotFound() throws Exception {
        mockMvc.perform(get("/api/persons/UnknownKnight"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    void testUpdatePerson() throws Exception {
        mockMvc.perform(post("/api/persons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Gawain\", \"age\": 24}"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/persons/Gawain").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Gawain\", \"age\": 32}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gawain"))
                .andExpect(jsonPath("$.age").value(32));
    }

    @Test
    void testDeletePerson() throws Exception {
        mockMvc.perform(post("/api/persons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Galahad\", \"age\": 27}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/persons/Galahad"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/persons/Galahad"))
                .andExpect(status().isNotFound());
    }
}
