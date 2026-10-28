package com.bnp.kingmainandhiskingdom.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercise A5: the whole authorization matrix of the arsenal in one parameterized test:
 * 4 endpoints x 4 callers (anonymous, SUBJECT, KNIGHT, KING) = 16 combinations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArsenalSecurityMatrixTest {

    private static final String WEAPON = "{\"type\":\"WEAPON\",\"name\":\"Matrix sword\",\"weight\":1.0,\"damage\":10,\"damageType\":\"SLASHING\"}";
    private static final String CATAPULT = "{\"type\":\"CATAPULT\",\"name\":\"Matrix catapult\",\"crewSize\":2,\"rangeMeters\":50}";

    @Autowired
    private MockMvc mockMvc;

    static Stream<Arguments> matrix() {
        // method, url, body, role (null = anonymous), expected status
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/equipment", null, null, 401),
                Arguments.of(HttpMethod.GET, "/api/equipment", null, "SUBJECT", 200),
                Arguments.of(HttpMethod.GET, "/api/equipment", null, "KNIGHT", 200),
                Arguments.of(HttpMethod.GET, "/api/equipment", null, "KING", 200),

                Arguments.of(HttpMethod.POST, "/api/equipment", WEAPON, null, 401),
                Arguments.of(HttpMethod.POST, "/api/equipment", WEAPON, "SUBJECT", 403),
                Arguments.of(HttpMethod.POST, "/api/equipment", WEAPON, "KNIGHT", 201),
                Arguments.of(HttpMethod.POST, "/api/equipment", WEAPON, "KING", 201),

                Arguments.of(HttpMethod.GET, "/api/war-machines", null, null, 401),
                Arguments.of(HttpMethod.GET, "/api/war-machines", null, "SUBJECT", 200),
                Arguments.of(HttpMethod.GET, "/api/war-machines", null, "KNIGHT", 200),
                Arguments.of(HttpMethod.GET, "/api/war-machines", null, "KING", 200),

                Arguments.of(HttpMethod.POST, "/api/war-machines", CATAPULT, null, 401),
                Arguments.of(HttpMethod.POST, "/api/war-machines", CATAPULT, "SUBJECT", 403),
                Arguments.of(HttpMethod.POST, "/api/war-machines", CATAPULT, "KNIGHT", 403),
                Arguments.of(HttpMethod.POST, "/api/war-machines", CATAPULT, "KING", 201));
    }

    @ParameterizedTest(name = "{0} {1} as {3} -> {4}")
    @MethodSource("matrix")
    void authorizationMatrix(HttpMethod method, String url, String body, String role, int expected) throws Exception {
        MockHttpServletRequestBuilder builder = request(method, url);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        if (role != null) {
            // @WithMockUser cannot be parameterized: build the user per call instead
            builder.with(user("matrix-" + role.toLowerCase()).roles(role));
        }
        mockMvc.perform(builder).andExpect(status().is(expected));
    }
}
