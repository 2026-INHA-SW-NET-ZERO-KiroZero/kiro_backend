package com.kirozero.netzero.domain.ingredient.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/001_ingredient_master_seed.sql")
        );
        populator.execute(dataSource);
    }

    @Test
    void searchesIngredientMasterForSlotJoinScreen() throws Exception {
        mockMvc.perform(get("/api/v1/ingredients")
                        .param("keyword", "양배추"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredients[0].ingredientId").value(12))
                .andExpect(jsonPath("$.ingredients[0].nameKo").value("양배추"))
                .andExpect(jsonPath("$.ingredients[0].gramsPerCount").value(700.0));
    }
}
