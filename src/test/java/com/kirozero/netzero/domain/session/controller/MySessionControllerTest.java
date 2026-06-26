package com.kirozero.netzero.domain.session.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class MySessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM consumption_record_items");
        jdbcTemplate.update("DELETE FROM consumption_records");
        jdbcTemplate.update("DELETE FROM menu_votes");
        jdbcTemplate.update("DELETE FROM session_ingredients");
        jdbcTemplate.update("DELETE FROM session_participants");
        jdbcTemplate.update("DELETE FROM user_allergies");
        jdbcTemplate.update("DELETE FROM users");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/001_ingredient_master_seed.sql"),
                new ClassPathResource("db/seed/003_demo_slots_seed.sql")
        );
        populator.execute(dataSource);
    }

    @Test
    void listsMyJoinedSessionsForApplicationScreen() throws Exception {
        String token = signupAndGetToken("my-sessions@inha.edu");
        joinSlot(token, 1, 3, 1);
        joinSlot(token, 7, 12, 0.5);

        mockMvc.perform(get("/api/v1/me/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions.length()").value(2))
                .andExpect(jsonPath("$.sessions[0].slotId").value(7))
                .andExpect(jsonPath("$.sessions[0].participantId").isNumber())
                .andExpect(jsonPath("$.sessions[0].placeName").value("인하대 조리실습실"))
                .andExpect(jsonPath("$.sessions[0].stationCode").value("A"))
                .andExpect(jsonPath("$.sessions[0].timeLabel").value("18:00-20:00"))
                .andExpect(jsonPath("$.sessions[0].participantCount").value(1))
                .andExpect(jsonPath("$.sessions[0].myIngredientCount").value(1))
                .andExpect(jsonPath("$.sessions[0].canPurchase").value(true))
                .andExpect(jsonPath("$.sessions[0].hasRecommendation").value(false))
                .andExpect(jsonPath("$.sessions[0].hasSelectedMenu").value(false))
                .andExpect(jsonPath("$.sessions[0].completed").value(false));
    }

    @Test
    void readsMySessionDetailForApplicationDetailScreen() throws Exception {
        String token = signupAndGetToken("my-session-detail@inha.edu");
        joinSlot(token, 1, 3, 1);

        mockMvc.perform(get("/api/v1/me/sessions/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(1))
                .andExpect(jsonPath("$.joined").value(true))
                .andExpect(jsonPath("$.myParticipantId").isNumber())
                .andExpect(jsonPath("$.myIngredients.length()").value(1))
                .andExpect(jsonPath("$.myIngredients[0].nameKo").value("양파"))
                .andExpect(jsonPath("$.session.participants[0].nickname").value("내모임사용자"))
                .andExpect(jsonPath("$.session.participants[0].allergyTags[0]").value("egg"))
                .andExpect(jsonPath("$.session.sharedIngredientPool[0].nameKo").value("양파"));
    }

    private String signupAndGetToken(String email) throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password1234",
                                  "nickname": "내모임사용자",
                                  "cookingSkill": "MEDIUM",
                                  "allergyTags": ["egg"]
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return signupResult.getResponse().getContentAsString()
                .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private void joinSlot(String token, long slotId, long ingredientId, double count) throws Exception {
        mockMvc.perform(post("/api/v1/slots/{slotId}/join", slotId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "canPurchase": true,
                                  "ingredients": [
                                    {
                                      "ingredientId": %d,
                                      "count": %s
                                    }
                                  ]
                                }
                                """.formatted(ingredientId, count)))
                .andExpect(status().isOk());
    }
}
