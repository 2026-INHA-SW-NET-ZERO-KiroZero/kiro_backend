package com.kirozero.netzero.domain.slot.controller;

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
class SlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
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
}
