package com.kirozero.netzero.domain.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class DemoSeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void seedsDemoIngredientsAndSlots() throws Exception {
        mockMvc.perform(post("/api/v1/demo/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdIngredients").value(100))
                .andExpect(jsonPath("$.createdSlots").value(12))
                .andExpect(jsonPath("$.createdUsers").value(8));
    }

    @Test
    void seedsAlmostFullDemoSlotThatBecomesRecommendableAfterOneJoin() throws Exception {
        mockMvc.perform(post("/api/v1/demo/seed"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "demo1@inha.edu",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("감자손질러"));

        mockMvc.perform(get("/api/v1/sessions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.participants.length()").value(3))
                .andExpect(jsonPath("$.participants[0].nickname").value("감자손질러"))
                .andExpect(jsonPath("$.sharedIngredientPool.length()").value(6))
                .andExpect(jsonPath("$.canRequestRecommendation").value(false));

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "front-demo@inha.edu",
                                  "password": "password123",
                                  "nickname": "데모촬영자",
                                  "cookingSkill": "MEDIUM",
                                  "allergyTags": []
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token = signupResult.getResponse().getContentAsString()
                .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/v1/slots/1/join")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "canPurchase": true,
                                  "ingredients": [
                                    {
                                      "ingredientId": 34,
                                      "count": 2
                                    },
                                    {
                                      "ingredientId": 7,
                                      "count": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(1))
                .andExpect(jsonPath("$.ingredients.length()").value(2));

        mockMvc.perform(get("/api/v1/sessions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(4))
                .andExpect(jsonPath("$.sharedIngredientPool.length()").value(8))
                .andExpect(jsonPath("$.canRequestRecommendation").value(true));
    }
}
