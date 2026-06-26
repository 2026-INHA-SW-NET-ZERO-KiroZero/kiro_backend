package com.kirozero.netzero.domain.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.kirozero.netzero.domain.ai.model.AiParticipant;
import com.kirozero.netzero.domain.ai.model.CookingGuideGenerationContext;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import com.kirozero.netzero.domain.recommendation.dto.CandidateUsedIngredientResponse;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CookingGuideResponseParserTest {

    @Test
    void parsesFencedWrappedSnakeCaseResponseAndAddsMissingParticipantTask() {
        CookingGuideResponse guide = CookingGuideResponseParser.parseOrFallback("""
                ```json
                {
                  "cooking_guide": {
                    "slot_id": 999,
                    "menu_name": "다른 메뉴명",
                    "steps": [
                      {
                        "step_order": "1",
                        "phase": "prep",
                        "title": "양배추 손질",
                        "estimated_minutes": "7",
                        "instruction": "양배추를 먼저 손질합니다.",
                        "used_ingredients": [
                          {
                            "ingredient_id": 12,
                            "name_ko": "양배추",
                            "planned_use_grams": "280"
                          }
                        ],
                        "tools": ["도마", "칼"],
                        "kit_items": ["소금"],
                        "participant_tasks": [
                          {
                            "participant_id": 1,
                            "nickname": "A",
                            "task_name": "양배추 채썰기",
                            "task_detail": "양배추 280g을 얇게 채 썹니다."
                          }
                        ]
                      }
                    ]
                  }
                }
                ```
                """, context());

        assertEquals(1L, guide.slotId());
        assertEquals("양배추 감자 볶음", guide.menuName());
        assertEquals(1, guide.steps().size());
        assertEquals("PREP", guide.steps().getFirst().phase());
        assertEquals(2, guide.steps().getFirst().participantTasks().size());
        assertEquals("양배추 채썰기", guide.steps().getFirst().participantTasks().getFirst().taskName());
        assertEquals(2L, guide.steps().getFirst().participantTasks().get(1).participantId());
        assertFalse(guide.steps().getFirst().participantTasks().get(1).displayInstruction().isBlank());
    }

    @Test
    void returnsDeterministicFallbackGuideWhenClaudeTextHasNoJson() {
        CookingGuideResponse guide = CookingGuideResponseParser.parseOrFallback("JSON이 아닌 응답입니다.", context());

        assertEquals(1L, guide.slotId());
        assertEquals("양배추 감자 볶음", guide.menuName());
        assertEquals(3, guide.steps().size());
        assertEquals("PREP", guide.steps().get(0).phase());
        assertEquals("COOK", guide.steps().get(1).phase());
        assertEquals("FINISH", guide.steps().get(2).phase());
        assertEquals(2, guide.steps().get(0).participantTasks().size());
        assertEquals(2, guide.steps().get(1).participantTasks().size());
        assertEquals(2, guide.steps().get(2).participantTasks().size());
    }

    @Test
    void fallbackGuideContainsConcreteCookableInstructions() {
        CookingGuideResponse guide = CookingGuideResponseParser.parseOrFallback("JSON이 아닌 응답입니다.", context());

        var prepTask = guide.steps().get(0).participantTasks().getFirst();
        assertEquals("양배추 손질", prepTask.taskName());
        assertContains(prepTask.displayInstruction(), "양배추 280g");
        assertContains(prepTask.displayInstruction(), "0.5cm");

        var cookStep = guide.steps().get(1);
        assertContains(cookStep.instruction(), "중불");
        assertContains(cookStep.instruction(), "3분");

        var finishTask = guide.steps().get(2).participantTasks().getFirst();
        assertContains(finishTask.displayInstruction(), "완성 사진");
        assertContains(finishTask.displayInstruction(), "소진량");
    }

    private CookingGuideGenerationContext context() {
        MenuCandidateResponse selectedMenu = new MenuCandidateResponse(
                "C",
                "양배추 감자 볶음",
                "LOW_CARBON",
                List.of(
                        new CandidateUsedIngredientResponse(
                                12L,
                                "양배추",
                                new BigDecimal("350"),
                                new BigDecimal("280"),
                                new BigDecimal("0.8")
                        ),
                        new CandidateUsedIngredientResponse(
                                8L,
                                "감자",
                                new BigDecimal("300"),
                                new BigDecimal("240"),
                                new BigDecimal("0.8")
                        )
                ),
                List.of("식용유", "간장"),
                List.of(),
                30,
                "LOW",
                "남은 채소를 많이 사용할 수 있습니다.",
                List.of("재료 손질", "볶기", "담기"),
                List.of("손질", "조리")
        );

        return new CookingGuideGenerationContext(
                1L,
                selectedMenu,
                List.of(
                        new AiParticipant(1L, "A", "LOW", false, List.of()),
                        new AiParticipant(2L, "B", "HIGH", true, List.of())
                )
        );
    }

    private void assertContains(String actual, String expected) {
        assertFalse(actual == null || actual.isBlank());
        org.junit.jupiter.api.Assertions.assertTrue(
                actual.contains(expected),
                () -> "Expected <%s> to contain <%s>".formatted(actual, expected)
        );
    }
}
