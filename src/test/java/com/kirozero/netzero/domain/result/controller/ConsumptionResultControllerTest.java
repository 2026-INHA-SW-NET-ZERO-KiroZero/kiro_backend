package com.kirozero.netzero.domain.result.controller;

import static org.hamcrest.Matchers.hasSize;
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
class ConsumptionResultControllerTest {

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
        jdbcTemplate.update("DELETE FROM slots");
        jdbcTemplate.update("DELETE FROM ingredient_master");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/001_ingredient_master_seed.sql"),
                new ClassPathResource("db/seed/003_demo_slots_seed.sql")
        );
        populator.execute(dataSource);
    }

    @Test
    void returnsReportScreenSummaryWithMonthlyTrend() throws Exception {
        SignupResult me = signup("report-me@inha.edu", "나");
        SignupResult mateA = signup("report-mate-a@inha.edu", "친구A");
        SignupResult mateB = signup("report-mate-b@inha.edu", "친구B");

        insertCompletedMaySession(me.userId(), mateA.userId(), mateB.userId());
        insertCompletedJuneSession(me.userId(), mateA.userId());

        mockMvc.perform(get("/api/v1/me/results/total")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + me.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedSessionCount").value(2))
                .andExpect(jsonPath("$.totalUsedGrams").value(850.0))
                .andExpect(jsonPath("$.totalEstimatedCarbonSavedKgco2e").value(2.0))
                .andExpect(jsonPath("$.totalRefundAmount").value(1300))
                .andExpect(jsonPath("$.togetherPeopleCount").value(5))
                .andExpect(jsonPath("$.providedIngredientCount").value(5))
                .andExpect(jsonPath("$.usedIngredientCount").value(4))
                .andExpect(jsonPath("$.averageIngredientUseRate").value(63))
                .andExpect(jsonPath("$.currentMonthEstimatedCarbonSavedKgco2e").value(0.8))
                .andExpect(jsonPath("$.previousMonthEstimatedCarbonSavedKgco2e").value(1.2))
                .andExpect(jsonPath("$.monthOverMonthCarbonDeltaKgco2e").value(-0.4))
                .andExpect(jsonPath("$.insightMessage").value("약 2.0kg의 탄소 배출을 줄인 셈이에요. 작은 한 끼가 모여 캠퍼스를 바꿔요."))
                .andExpect(jsonPath("$.monthlyResults", hasSize(6)))
                .andExpect(jsonPath("$.monthlyResults[0].yearMonth").value("2026-01"))
                .andExpect(jsonPath("$.monthlyResults[0].completedSessionCount").value(0))
                .andExpect(jsonPath("$.monthlyResults[0].totalUsedGrams").value(0))
                .andExpect(jsonPath("$.monthlyResults[4].yearMonth").value("2026-05"))
                .andExpect(jsonPath("$.monthlyResults[4].monthLabel").value("5월"))
                .andExpect(jsonPath("$.monthlyResults[5].yearMonth").value("2026-06"))
                .andExpect(jsonPath("$.monthlyResults[5].monthLabel").value("6월"))
                .andExpect(jsonPath("$.monthlyResults[5].completedSessionCount").value(1))
                .andExpect(jsonPath("$.monthlyResults[5].togetherPeopleCount").value(2))
                .andExpect(jsonPath("$.monthlyResults[5].providedIngredientCount").value(2))
                .andExpect(jsonPath("$.monthlyResults[5].usedIngredientCount").value(2))
                .andExpect(jsonPath("$.monthlyResults[5].averageIngredientUseRate").value(75))
                .andExpect(jsonPath("$.monthlyResults[5].totalEstimatedCarbonSavedKgco2e").value(0.8));
    }

    @Test
    void returnsZeroFilledMonthlyResultsForUserWithoutCompletedSessions() throws Exception {
        SignupResult me = signup("report-empty@inha.edu", "빈리포트");

        mockMvc.perform(get("/api/v1/me/results/total")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + me.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedSessionCount").value(0))
                .andExpect(jsonPath("$.monthlyResults", hasSize(6)))
                .andExpect(jsonPath("$.monthlyResults[0].yearMonth").value("2026-01"))
                .andExpect(jsonPath("$.monthlyResults[0].monthLabel").value("1월"))
                .andExpect(jsonPath("$.monthlyResults[0].completedSessionCount").value(0))
                .andExpect(jsonPath("$.monthlyResults[0].totalEstimatedCarbonSavedKgco2e").value(0))
                .andExpect(jsonPath("$.monthlyResults[5].yearMonth").value("2026-06"))
                .andExpect(jsonPath("$.monthlyResults[5].monthLabel").value("6월"))
                .andExpect(jsonPath("$.monthlyResults[5].completedSessionCount").value(0));
    }

    @Test
    void submitsConsumptionRecordAndAddsRefundCashToParticipants() throws Exception {
        SignupResult me = signup("cash-me@inha.edu", "나");
        SignupResult mate = signup("cash-mate@inha.edu", "친구");

        Long mySessionIngredientId = joinSlot(me.token(), 1, 12, 1);
        Long mateSessionIngredientId = joinSlot(mate.token(), 1, 3, 1);
        jdbcTemplate.update("""
                UPDATE slots
                SET status = 'MENU_PROPOSED',
                    selected_menu_json = ?
                WHERE id = 1
                """, """
                {
                  "candidateLabel": "C",
                  "menuName": "양배추 양파 볶음",
                  "menuType": "LOW_CARBON",
                  "usedLeftoverIngredients": [],
                  "commonKitItems": ["식용유", "간장"],
                  "purchaseItems": [],
                  "cookingTimeMinutes": 30,
                  "difficulty": "LOW",
                  "recommendationReason": "남은 채소를 많이 사용할 수 있음",
                  "cookingOutlineSteps": ["손질", "볶기"],
                  "rolePlanSummary": ["손질", "조리"]
                }
                """);

        mockMvc.perform(post("/api/v1/sessions/1/consumption-records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + me.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "finishedFoodRate": 100,
                                  "cookedPhotoUrl": "https://example.com/cooked.jpg",
                                  "afterPhotoUrl": "https://example.com/after.jpg",
                                  "items": [
                                    {
                                      "sessionIngredientId": %d,
                                      "useRate": 100
                                    },
                                    {
                                      "sessionIngredientId": %d,
                                      "useRate": 100
                                    }
                                  ]
                                }
                                """.formatted(mySessionIngredientId, mateSessionIngredientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundScore").value(100))
                .andExpect(jsonPath("$.refundAmountPerUser").value(1000));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + me.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(1000));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mate.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(1000));
    }

    private SignupResult signup(String email, String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password1234",
                                  "nickname": "%s",
                                  "cookingSkill": "MEDIUM",
                                  "allergyTags": []
                                }
                                """.formatted(email, nickname)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        Long userId = Long.parseLong(body.replaceAll(".*\\\"userId\\\":([0-9]+).*", "$1"));
        String token = body.replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
        return new SignupResult(userId, token);
    }

    private Long joinSlot(String token, long slotId, long ingredientId, int count) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/slots/{slotId}/join", slotId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "canPurchase": true,
                                  "ingredients": [
                                    {
                                      "ingredientId": %d,
                                      "count": %d
                                    }
                                  ]
                                }
                                """.formatted(ingredientId, count)))
                .andExpect(status().isOk())
                .andReturn();

        return Long.parseLong(result.getResponse().getContentAsString()
                .replaceAll(".*\\\"sessionIngredientId\\\":([0-9]+).*", "$1"));
    }

    private void insertCompletedMaySession(Long me, Long mateA, Long mateB) {
        jdbcTemplate.update("INSERT INTO session_participants (id, can_purchase, joined_at, slot_id, user_id, created_at, updated_at) VALUES (101, true, '2026-05-20 10:00:00', 1, ?, '2026-05-20 10:00:00', '2026-05-20 10:00:00')", me);
        jdbcTemplate.update("INSERT INTO session_participants (id, can_purchase, joined_at, slot_id, user_id, created_at, updated_at) VALUES (102, false, '2026-05-20 10:01:00', 1, ?, '2026-05-20 10:01:00', '2026-05-20 10:01:00')", mateA);
        jdbcTemplate.update("INSERT INTO session_participants (id, can_purchase, joined_at, slot_id, user_id, created_at, updated_at) VALUES (103, false, '2026-05-20 10:02:00', 1, ?, '2026-05-20 10:02:00', '2026-05-20 10:02:00')", mateB);
        jdbcTemplate.update("INSERT INTO session_ingredients (id, count, estimated_grams, ingredient_id, known_grams, participant_id, slot_id, created_at, updated_at) VALUES (1001, 1, 400, 12, 400, 101, 1, '2026-05-20 10:05:00', '2026-05-20 10:05:00')");
        jdbcTemplate.update("INSERT INTO session_ingredients (id, count, estimated_grams, ingredient_id, known_grams, participant_id, slot_id, created_at, updated_at) VALUES (1002, 1, 200, 3, 200, 101, 1, '2026-05-20 10:05:00', '2026-05-20 10:05:00')");
        jdbcTemplate.update("INSERT INTO session_ingredients (id, count, estimated_grams, ingredient_id, known_grams, participant_id, slot_id, created_at, updated_at) VALUES (1003, 1, 300, 44, 300, 102, 1, '2026-05-20 10:05:00', '2026-05-20 10:05:00')");
        jdbcTemplate.update("INSERT INTO consumption_records (id, slot_id, submitted_by, finished_food_rate, cooked_photo_url, after_photo_url, total_used_grams, avg_ingredient_use_rate, estimated_carbon_saved_kgco2e, low_carbon_selected, refund_score, refund_amount_per_user, created_at, updated_at) VALUES (501, 1, ?, 100, 'cooked-1.jpg', 'after-1.jpg', 500, 50, 1.2000, true, 60, 600, '2026-05-20 12:00:00', '2026-05-20 12:00:00')", me);
        jdbcTemplate.update("INSERT INTO consumption_record_items (id, record_id, session_ingredient_id, use_rate, used_grams, estimated_carbon_saved_kgco2e, created_at, updated_at) VALUES (2001, 501, 1001, 100, 400, 0.8000, '2026-05-20 12:01:00', '2026-05-20 12:01:00')");
        jdbcTemplate.update("INSERT INTO consumption_record_items (id, record_id, session_ingredient_id, use_rate, used_grams, estimated_carbon_saved_kgco2e, created_at, updated_at) VALUES (2002, 501, 1002, 50, 100, 0.4000, '2026-05-20 12:01:00', '2026-05-20 12:01:00')");
        jdbcTemplate.update("INSERT INTO consumption_record_items (id, record_id, session_ingredient_id, use_rate, used_grams, estimated_carbon_saved_kgco2e, created_at, updated_at) VALUES (2003, 501, 1003, 0, 0, 0.0000, '2026-05-20 12:01:00', '2026-05-20 12:01:00')");
    }

    private void insertCompletedJuneSession(Long me, Long mateA) {
        jdbcTemplate.update("INSERT INTO session_participants (id, can_purchase, joined_at, slot_id, user_id, created_at, updated_at) VALUES (201, true, '2026-06-15 10:00:00', 7, ?, '2026-06-15 10:00:00', '2026-06-15 10:00:00')", me);
        jdbcTemplate.update("INSERT INTO session_participants (id, can_purchase, joined_at, slot_id, user_id, created_at, updated_at) VALUES (202, false, '2026-06-15 10:01:00', 7, ?, '2026-06-15 10:01:00', '2026-06-15 10:01:00')", mateA);
        jdbcTemplate.update("INSERT INTO session_ingredients (id, count, estimated_grams, ingredient_id, known_grams, participant_id, slot_id, created_at, updated_at) VALUES (3001, 1, 100, 21, 100, 201, 7, '2026-06-15 10:05:00', '2026-06-15 10:05:00')");
        jdbcTemplate.update("INSERT INTO session_ingredients (id, count, estimated_grams, ingredient_id, known_grams, participant_id, slot_id, created_at, updated_at) VALUES (3002, 2, 500, 1, 500, 202, 7, '2026-06-15 10:05:00', '2026-06-15 10:05:00')");
        jdbcTemplate.update("INSERT INTO consumption_records (id, slot_id, submitted_by, finished_food_rate, cooked_photo_url, after_photo_url, total_used_grams, avg_ingredient_use_rate, estimated_carbon_saved_kgco2e, low_carbon_selected, refund_score, refund_amount_per_user, created_at, updated_at) VALUES (502, 7, ?, 100, 'cooked-2.jpg', 'after-2.jpg', 350, 75, 0.8000, true, 70, 700, '2026-06-15 12:00:00', '2026-06-15 12:00:00')", me);
        jdbcTemplate.update("INSERT INTO consumption_record_items (id, record_id, session_ingredient_id, use_rate, used_grams, estimated_carbon_saved_kgco2e, created_at, updated_at) VALUES (4001, 502, 3001, 100, 100, 0.3000, '2026-06-15 12:01:00', '2026-06-15 12:01:00')");
        jdbcTemplate.update("INSERT INTO consumption_record_items (id, record_id, session_ingredient_id, use_rate, used_grams, estimated_carbon_saved_kgco2e, created_at, updated_at) VALUES (4002, 502, 3002, 50, 250, 0.5000, '2026-06-15 12:01:00', '2026-06-15 12:01:00')");
    }

    private record SignupResult(Long userId, String token) {
    }
}
