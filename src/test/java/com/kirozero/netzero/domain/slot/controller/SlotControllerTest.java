package com.kirozero.netzero.domain.slot.controller;

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
class SlotControllerTest {

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
    void listsOpenSlotsForSlotListScreen() throws Exception {
        mockMvc.perform(get("/api/v1/slots")
                        .param("date", "2026-06-29")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(12))
                .andExpect(jsonPath("$.slots[0].slotId").value(1))
                .andExpect(jsonPath("$.slots[0].placeName").value("인하대 조리실습실"))
                .andExpect(jsonPath("$.slots[0].stationCode").value("A"))
                .andExpect(jsonPath("$.slots[0].startTime").value("16:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("18:00"))
                .andExpect(jsonPath("$.slots[0].participantCount").value(0))
                .andExpect(jsonPath("$.slots[0].commonKitSummary[0]").value("식용유"));
    }

    @Test
    void readsSlotDetailForJoinScreen() throws Exception {
        mockMvc.perform(get("/api/v1/slots/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(1))
                .andExpect(jsonPath("$.placeName").value("인하대 조리실습실"))
                .andExpect(jsonPath("$.stationCode").value("A"))
                .andExpect(jsonPath("$.participantCount").value(0))
                .andExpect(jsonPath("$.joined").value(false))
                .andExpect(jsonPath("$.myParticipantId").doesNotExist())
                .andExpect(jsonPath("$.commonKit[4]").value("참기름"))
                .andExpect(jsonPath("$.participants.length()").value(0));
    }

    @Test
    void joinsSlotWithAtLeastOneIngredientAndReflectsParticipantInDetail() throws Exception {
        String token = signupAndGetToken("slot-join@inha.edu");

        mockMvc.perform(post("/api/v1/slots/1/join")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "canPurchase": true,
                                  "ingredients": [
                                    {
                                      "ingredientId": 12,
                                      "count": 0.5,
                                      "knownGrams": 350
                                    },
                                    {
                                      "ingredientId": 3,
                                      "count": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(1))
                .andExpect(jsonPath("$.participantId").isNumber())
                .andExpect(jsonPath("$.canPurchase").value(true))
                .andExpect(jsonPath("$.ingredients.length()").value(2))
                .andExpect(jsonPath("$.ingredients[0].nameKo").value("양배추"))
                .andExpect(jsonPath("$.ingredients[0].estimatedGrams").value(350))
                .andExpect(jsonPath("$.ingredients[1].nameKo").value("양파"))
                .andExpect(jsonPath("$.ingredients[1].estimatedGrams").value(200.00));

        mockMvc.perform(get("/api/v1/slots/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.participants[0].nickname").value("슬롯참여자"))
                .andExpect(jsonPath("$.participants[0].ingredientCount").value(2));
    }

    @Test
    void readsAuthenticatedSlotDetailWithMyJoinStateAndParticipantAllergies() throws Exception {
        String token = signupAndGetToken("slot-auth-detail@inha.edu", "계란알러지", "[\"egg\", \"milk\"]");

        MvcResult joinResult = mockMvc.perform(post("/api/v1/slots/1/join")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "canPurchase": false,
                                  "ingredients": [
                                    {
                                      "ingredientId": 3,
                                      "count": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String participantId = joinResult.getResponse().getContentAsString()
                .replaceAll(".*\\\"participantId\\\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/v1/slots/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joined").value(true))
                .andExpect(jsonPath("$.myParticipantId").value(Integer.parseInt(participantId)))
                .andExpect(jsonPath("$.participants[0].nickname").value("계란알러지"))
                .andExpect(jsonPath("$.participants[0].allergyTags[0]").value("egg"))
                .andExpect(jsonPath("$.participants[0].allergyTags[1]").value("milk"));
    }

    @Test
    void rejectsJoinWithoutIngredients() throws Exception {
        String token = signupAndGetToken("slot-empty-ingredients@inha.edu");

        mockMvc.perform(post("/api/v1/slots/1/join")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "canPurchase": false,
                                  "ingredients": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private String signupAndGetToken(String email) throws Exception {
        return signupAndGetToken(email, "슬롯참여자", "[]");
    }

    private String signupAndGetToken(String email, String nickname, String allergyTags) throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password1234",
                                  "nickname": "%s",
                                  "cookingSkill": "MEDIUM",
                                  "allergyTags": %s
                                }
                                """.formatted(email, nickname, allergyTags)))
                .andExpect(status().isOk())
                .andReturn();

        return signupResult.getResponse().getContentAsString()
                .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
