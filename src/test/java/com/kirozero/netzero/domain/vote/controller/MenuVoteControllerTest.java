package com.kirozero.netzero.domain.vote.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import java.util.List;
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
class MenuVoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void waitsForAllParticipantsBeforeConfirmingEvenWhenCandidateGetsTwoVotes() throws Exception {
        List<String> tokens = joinFourParticipants(1);
        proposeCandidates(1);

        vote(tokens.get(0), 1, """
                {
                  "voteType": "A",
                  "candidateLabel": "A"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(false));

        vote(tokens.get(1), 1, """
                {
                  "voteType": "A",
                  "candidateLabel": "A"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(false))
                .andExpect(jsonPath("$.nextStatus").value("MENU_PROPOSED"));
    }

    @Test
    void confirmsCandidateCloserToDWhenVoteIsTwoToTwo() throws Exception {
        List<String> tokens = joinFourParticipants(1);
        proposeCandidates(1);

        vote(tokens.get(0), 1, voteBody("A")).andExpect(status().isOk());
        vote(tokens.get(1), 1, voteBody("A")).andExpect(status().isOk());
        vote(tokens.get(2), 1, voteBody("D")).andExpect(status().isOk());
        vote(tokens.get(3), 1, voteBody("D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true))
                .andExpect(jsonPath("$.confirmedCandidateLabel").value("D"))
                .andExpect(jsonPath("$.selectedMenu.menuType").value("LOW_CARBON"));
    }

    @Test
    void reopensSlotForRecommendationWhenRegenerationGetsTwoVotes() throws Exception {
        List<String> tokens = joinFourParticipants(1);
        proposeCandidates(1);

        vote(tokens.get(0), 1, eVoteBody("메뉴가 너무 무거워요")).andExpect(status().isOk());
        vote(tokens.get(1), 1, eVoteBody("채소 위주로 다시 보고 싶어요")).andExpect(status().isOk());
        vote(tokens.get(2), 1, voteBody("A")).andExpect(status().isOk());
        vote(tokens.get(3), 1, voteBody("B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(false))
                .andExpect(jsonPath("$.selectedMenu").doesNotExist())
                .andExpect(jsonPath("$.nextStatus").value("OPEN"));
    }

    private List<String> joinFourParticipants(long slotId) throws Exception {
        return List.of(
                signupAndJoin("vote-a@inha.edu", slotId, 3),
                signupAndJoin("vote-b@inha.edu", slotId, 8),
                signupAndJoin("vote-c@inha.edu", slotId, 12),
                signupAndJoin("vote-d@inha.edu", slotId, 20)
        );
    }

    private String signupAndJoin(String email, long slotId, long ingredientId) throws Exception {
        String token = signupAndGetToken(email);
        mockMvc.perform(post("/api/v1/slots/{slotId}/join", slotId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "canPurchase": true,
                                  "ingredients": [
                                    {
                                      "ingredientId": %d,
                                      "count": 1
                                    }
                                  ]
                                }
                                """.formatted(ingredientId)))
                .andExpect(status().isOk());
        return token;
    }

    private String signupAndGetToken(String email) throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password1234",
                                  "nickname": "%s",
                                  "cookingSkill": "MEDIUM",
                                  "allergyTags": []
                                }
                                """.formatted(email, email.substring(0, email.indexOf('@')))))
                .andExpect(status().isOk())
                .andReturn();

        return signupResult.getResponse().getContentAsString()
                .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private void proposeCandidates(long slotId) throws Exception {
        String candidatesJson = objectMapper.writeValueAsString(List.of(
                candidate("A", "GENERAL"),
                candidate("B", "GENERAL"),
                candidate("C", "LOW_CARBON"),
                candidate("D", "LOW_CARBON")
        ));
        jdbcTemplate.update("""
                        UPDATE slots
                        SET status = 'MENU_PROPOSED',
                            candidates_json = ?,
                            selected_menu_json = NULL,
                            recommendation_count = 1
                        WHERE id = ?
                        """,
                candidatesJson,
                slotId
        );
    }

    private MenuCandidateResponse candidate(String label, String type) {
        return new MenuCandidateResponse(
                label,
                label + " 메뉴",
                type,
                List.of(),
                List.of(),
                List.of(),
                30,
                "LOW",
                "테스트",
                List.of(),
                List.of()
        );
    }

    private org.springframework.test.web.servlet.ResultActions vote(String token, long slotId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/sessions/{slotId}/votes", slotId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String voteBody(String label) {
        return """
                {
                  "voteType": "%s",
                  "candidateLabel": "%s"
                }
                """.formatted(label, label);
    }

    private String eVoteBody(String reason) {
        return """
                {
                  "voteType": "E",
                  "reasonText": "%s"
                }
                """.formatted(reason);
    }
}
