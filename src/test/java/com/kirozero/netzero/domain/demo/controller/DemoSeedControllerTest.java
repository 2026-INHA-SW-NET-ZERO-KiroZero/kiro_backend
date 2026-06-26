package com.kirozero.netzero.domain.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
                .andExpect(jsonPath("$.createdUsers").value(0));
    }
}
