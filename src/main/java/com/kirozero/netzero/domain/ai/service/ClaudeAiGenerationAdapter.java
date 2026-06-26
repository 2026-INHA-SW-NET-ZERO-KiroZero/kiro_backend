package com.kirozero.netzero.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.ai.config.AiGenerationProperties;
import com.kirozero.netzero.domain.ai.enums.AiProvider;
import com.kirozero.netzero.domain.ai.exception.AiGenerationException;
import com.kirozero.netzero.domain.ai.model.CookingGuideGenerationContext;
import com.kirozero.netzero.domain.ai.model.MenuCandidateGenerationContext;
import com.kirozero.netzero.domain.ai.port.AiGenerationAdapter;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class ClaudeAiGenerationAdapter implements AiGenerationAdapter {

    private static final String SYSTEM_PROMPT = """
            당신은 냉장고 반상회 서비스의 요리 추천 엔진입니다.
            사용자의 남은 식재료를 실제 조리 행동으로 연결하는 것이 목표입니다.
            반드시 요청한 JSON 스키마만 반환하고, 설명 문장이나 마크다운을 붙이지 마세요.
            알레르기 태그와 구매 가능자를 존중하고, 저탄소 후보에는 육류/어패류 추가구매를 넣지 마세요.
            """;

    private final AiGenerationProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiProvider provider() {
        return AiProvider.CLAUDE;
    }

    @Override
    public List<MenuCandidateResponse> generateMenuCandidates(MenuCandidateGenerationContext context) {
        String prompt = """
                아래 세션 정보를 기반으로 메뉴 후보 4개를 생성하세요.

                조건:
                - JSON 배열만 반환합니다.
                - candidateLabel은 정확히 A, B, C, D입니다.
                - A와 B는 menuType GENERAL입니다.
                - C와 D는 menuType LOW_CARBON입니다.
                - usedLeftoverIngredients는 입력된 ingredientId만 사용합니다.
                - plannedUseGrams는 availableGrams를 넘지 않습니다.
                - purchaseItems는 추가구매 가능자가 있을 때만 넣습니다.
                - LOW_CARBON 후보의 purchaseItems에는 육류/어패류를 넣지 않습니다.
                - commonKitItems는 입력된 공용 키트 범위에서만 선택합니다.

                각 객체 스키마:
                {
                  "candidateLabel": "A",
                  "menuName": "메뉴명",
                  "menuType": "GENERAL",
                  "usedLeftoverIngredients": [
                    {
                      "ingredientId": 1,
                      "nameKo": "양배추",
                      "availableGrams": 350.0,
                      "plannedUseGrams": 280.0,
                      "estimatedUseRatio": 0.8
                    }
                  ],
                  "commonKitItems": ["식용유", "간장"],
                  "purchaseItems": [
                    {
                      "name": "계란",
                      "category": "EGG",
                      "quantityGrams": 240.0,
                      "allergenTags": ["egg"],
                      "assignedToNickname": "구매가능자 닉네임",
                      "estimatedCost": 3000
                    }
                  ],
                  "cookingTimeMinutes": 35,
                  "difficulty": "LOW",
                  "recommendationReason": "왜 이 메뉴가 적합한지",
                  "cookingOutlineSteps": ["요약 단계 1", "요약 단계 2"],
                  "rolePlanSummary": ["역할 요약 1", "역할 요약 2"]
                }

                세션 정보:
                %s
                """.formatted(writeJson(context));

        String text = requestMessage(prompt);
        List<MenuCandidateResponse> candidates = readMenuCandidates(text);
        AiResponseValidator.validateMenuCandidates(candidates);
        return candidates;
    }

    @Override
    public CookingGuideResponse generateCookingGuide(CookingGuideGenerationContext context) {
        String prompt = """
                아래 확정 메뉴와 참여자 정보를 기반으로 조리 진행 가이드를 생성하세요.

                조건:
                - JSON 객체만 반환합니다.
                - slotId와 menuName은 입력값과 같아야 합니다.
                - steps는 PREP, COOK, FINISH를 포함하되 필요하면 더 세분화할 수 있습니다.
                - 각 step에는 참여자 전원의 participantTasks를 넣습니다.
                - 각자의 역할이 같은 시간대에 겹치지 않게 배정합니다.
                - taskDetail과 displayInstruction은 친절하고 구체적으로 작성합니다.
                - "이 단계에서 왜 하는지, 어떤 재료를 몇 g 쓰는지, 어떻게 처리하는지, 주의할 점"을 포함합니다.
                - participantId와 nickname은 입력값을 그대로 사용합니다.

                반환 스키마:
                {
                  "slotId": 1,
                  "menuName": "메뉴명",
                  "steps": [
                    {
                      "stepOrder": 1,
                      "phase": "PREP",
                      "title": "양배추와 양파 손질",
                      "estimatedMinutes": 8,
                      "instruction": "전체 단계 설명",
                      "usedIngredients": [
                        {
                          "ingredientId": 1,
                          "nameKo": "양배추",
                          "plannedUseGrams": 280.0
                        }
                      ],
                      "tools": ["도마", "칼"],
                      "kitItems": ["소금"],
                      "participantTasks": [
                        {
                          "participantId": 10,
                          "nickname": "닉네임",
                          "taskName": "양배추 채썰기",
                          "taskPurpose": "볶을 때 숨이 고르게 죽도록",
                          "taskDetail": "양배추 280g을 0.5cm 폭으로 채 썹니다.",
                          "attentionPoints": ["심지를 제거하고 채 썹니다."],
                          "displayInstruction": "볶을 때 숨이 고르게 죽도록 양배추 280g을 0.5cm 폭으로 채 썹니다.",
                          "skillRequired": "LOW"
                        }
                      ],
                      "safetyNote": "안전 주의",
                      "completionCriteria": "완료 기준"
                    }
                  ]
                }

                입력 정보:
                %s
                """.formatted(writeJson(context));

        String text = requestMessage(prompt);
        CookingGuideResponse guide = readCookingGuide(text);
        AiResponseValidator.validateCookingGuide(guide);
        return guide;
    }

    private String requestMessage(String prompt) {
        AiGenerationProperties.Claude claude = properties.getClaude();
        if (!StringUtils.hasText(claude.getApiKey())) {
            throw new AiGenerationException("Claude API key is not configured.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", claude.getModel());
        body.put("max_tokens", claude.getMaxTokens());
        body.put("system", SYSTEM_PROMPT);
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", prompt
        )));

        try {
            JsonNode response = RestClient.builder()
                    .baseUrl(claude.getBaseUrl())
                    .defaultHeader("x-api-key", claude.getApiKey())
                    .defaultHeader("anthropic-version", claude.getVersion())
                    .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return extractFirstText(response);
        } catch (RestClientException e) {
            throw new AiGenerationException("Claude API request failed.", e);
        }
    }

    private String extractFirstText(JsonNode response) {
        if (response == null || !response.has("content") || !response.get("content").isArray()) {
            throw new AiGenerationException("Claude API response has no content array.");
        }
        for (JsonNode block : response.get("content")) {
            if ("text".equals(block.path("type").asText()) && StringUtils.hasText(block.path("text").asText())) {
                return block.path("text").asText();
            }
        }
        throw new AiGenerationException("Claude API response has no text content.");
    }

    private List<MenuCandidateResponse> readMenuCandidates(String text) {
        try {
            JsonNode root = objectMapper.readTree(extractJsonPayload(text));
            JsonNode candidatesNode = root.isArray() ? root : root.path("candidates");
            if (!candidatesNode.isArray()) {
                throw new AiGenerationException("Claude menu response must be an array or contain candidates array.");
            }
            return objectMapper.convertValue(candidatesNode, new TypeReference<>() {
            });
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new AiGenerationException("Claude menu response cannot be parsed.", e);
        }
    }

    private CookingGuideResponse readCookingGuide(String text) {
        try {
            return objectMapper.readValue(extractJsonPayload(text), CookingGuideResponse.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new AiGenerationException("Claude cooking guide response cannot be parsed.", e);
        }
    }

    private String extractJsonPayload(String text) {
        if (!StringUtils.hasText(text)) {
            throw new AiGenerationException("Claude response is blank.");
        }

        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
            }
        }

        int objectStart = cleaned.indexOf('{');
        int arrayStart = cleaned.indexOf('[');
        int start;
        if (objectStart < 0) {
            start = arrayStart;
        } else if (arrayStart < 0) {
            start = objectStart;
        } else {
            start = Math.min(objectStart, arrayStart);
        }

        int end = Math.max(cleaned.lastIndexOf('}'), cleaned.lastIndexOf(']'));
        if (start < 0 || end < start) {
            throw new AiGenerationException("Claude response does not contain JSON.");
        }
        return cleaned.substring(start, end + 1);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new AiGenerationException("AI prompt context cannot be serialized.", e);
        }
    }
}
