package com.kirozero.netzero.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.ai.config.AiGenerationProperties;
import com.kirozero.netzero.domain.ai.enums.AiProvider;
import com.kirozero.netzero.domain.ai.exception.AiGenerationException;
import com.kirozero.netzero.domain.ai.model.CookingGuideGenerationContext;
import com.kirozero.netzero.domain.ai.model.MenuCandidateGenerationContext;
import com.kirozero.netzero.domain.ai.model.RawMenuCandidate;
import com.kirozero.netzero.domain.ai.port.AiGenerationAdapter;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import com.kirozero.netzero.domain.recommendation.dto.CandidateUsedIngredientResponse;
import com.kirozero.netzero.domain.recommendation.dto.PurchaseItemResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class ClaudeAiGenerationAdapter implements AiGenerationAdapter {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAiGenerationAdapter.class);

    private static final String SYSTEM_PROMPT = """
            당신은 냉장고 반상회 서비스의 요리 추천 엔진입니다.
            사용자의 남은 식재료를 실제 조리 행동으로 연결하는 것이 목표입니다.
            반드시 요청한 JSON 스키마만 반환하고, 설명 문장이나 마크다운을 붙이지 마세요.
            저탄소 후보에는 육류/어패류 추가구매를 넣지 마세요.
            매우 중요: 모든 후보는 participants 중 누구 한 명이라도 알레르기를 가진 재료/추가구매를 절대 포함하면 안 됩니다.
            예를 들어 누군가 "egg" 알레르기면 계란/마요네즈 등 egg 태그 재료를 어떤 후보에도 쓰지 마세요.
            누군가 "soy" 알레르기면 두부/콩나물 등 soy 태그 재료를 어떤 후보에도 쓰지 마세요.
            sharedIngredients에 알레르기 충돌 재료가 들어 있어도, 그 재료는 후보에서 빼고 다른 재료로 6개를 만드세요.
            추가구매 담당 배정은 백엔드가 처리하니 그 부분만 비워 두세요.
            """;

    private final AiGenerationProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiProvider provider() {
        return AiProvider.CLAUDE;
    }

    @Override
    public List<RawMenuCandidate> generateMenuCandidates(MenuCandidateGenerationContext context) {
        String prompt = """
                아래 세션 정보를 기반으로 메뉴 후보 6개를 생성하세요.

                조건:
                - 최상위 JSON은 {"candidates": [...]} 형태이고, 배열에는 정확히 6개 객체를 넣습니다.
                - 6개 중 정확히 3개는 menuType "GENERAL", 3개는 "LOW_CARBON"입니다.
                - usedLeftoverIngredients의 ingredientId는 입력된 sharedIngredients의 ingredientId 중에서만 사용합니다.
                - plannedUseGrams는 availableGrams를 넘지 않습니다.
                - purchaseItems는 추가구매 가능자가 있을 때만 넣고, 없을 때는 빈 배열로 둡니다.
                - LOW_CARBON 후보의 purchaseItems에는 육류/어패류 카테고리를 넣지 않습니다.
                - commonKitItems는 입력된 commonKitItems 범위에서만 선택합니다.
                - candidateLabel과 assignedToNickname은 비워 두세요. 백엔드가 채웁니다.
                - 알레르기 회피: participants 배열의 모든 allergyTags 값을 합집합으로 모으세요.
                  그 합집합 안에 들어있는 태그(예: egg, milk, soy, crustacean_shellfish 등)와 충돌하는 재료는
                  6개 후보 어디에도 (usedLeftoverIngredients, purchaseItems 모두) 절대 넣지 마세요.
                  예: 합집합에 "egg"가 있으면 ingredientId 41(계란)이나 마요네즈 등 어떤 형태의 계란도 사용 금지.
                  예: 합집합에 "soy"가 있으면 ingredientId 44(두부), 4(콩나물) 등 콩 기반 재료 사용 금지.
                  sharedIngredients에 그런 재료가 있어도 그건 모두 무시하고 나머지 재료로만 6개를 만드세요.

                각 후보 객체 스키마:
                {
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
        List<RawMenuCandidate> candidates = readRawMenuCandidates(text);
        AiResponseValidator.validateRawCandidates(candidates);
        return candidates;
    }

    @Override
    public CookingGuideResponse generateCookingGuide(CookingGuideGenerationContext context) {
        String prompt = """
                아래 확정 메뉴와 참여자 정보를 기반으로 조리 진행 가이드를 생성하세요.

                조건:
                - JSON 객체만 반환합니다.
                - slotId와 menuName은 입력값과 같아야 합니다.
                - steps는 최소 5개 이상으로 작성합니다.
                - PREP는 2개 이상, COOK는 2개 이상, FINISH는 1개 이상 포함합니다.
                - 각 step은 사용자가 화면을 보고 바로 움직일 수 있는 조리 지시여야 합니다.
                - 각 step에는 참여자 전원의 participantTasks를 넣습니다.
                - 같은 step 안에서 참여자들의 역할이 겹치지 않게 배정합니다.
                - 숙련도 HIGH 참여자는 팬 조리, 불 조절, 간 조절 같은 위험/판단 작업을 우선 배정합니다.
                - 숙련도 LOW 참여자는 세척, 계량, 담기, 사진 기록, 정리처럼 안전한 작업을 우선 배정합니다.
                - taskName은 "담당"처럼 추상적으로 쓰지 말고 "양배추 0.5cm 채썰기", "중불에서 감자 3분 볶기"처럼 행동으로 씁니다.
                - taskDetail과 displayInstruction은 친절하고 구체적으로 작성합니다.
                - "이 단계에서 왜 하는지, 어떤 재료를 몇 g 쓰는지, 어떻게 처리하는지, 주의할 점"을 포함합니다.
                - 손질 단계에는 두께, 크기, 물기 제거, 도구를 포함합니다.
                - 조리 단계에는 불 세기, 예상 시간, 넣는 순서, 익힘 판단 기준을 포함합니다.
                - 마무리 단계에는 완성 사진, 식후 사진, 소진량 기록 안내를 포함합니다.
                - usedIngredients의 ingredientId는 selectedMenu.usedLeftoverIngredients 안에 있는 값만 사용합니다.
                - plannedUseGrams는 selectedMenu.usedLeftoverIngredients의 plannedUseGrams를 넘지 않습니다.
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
        log.debug("Claude cooking guide raw text: {}", text);
        CookingGuideResponse guide = CookingGuideResponseParser.parseOrFallback(text, context);
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
            String responseBody = RestClient.builder()
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
                    .body(String.class);

            return extractFirstText(responseBody);
        } catch (RestClientException e) {
            throw new AiGenerationException("Claude API request failed.", e);
        }
    }

    private String extractFirstText(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new AiGenerationException("Claude API response is empty.");
        }
        JsonNode response;
        try {
            response = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            throw new AiGenerationException("Claude API response is not valid JSON.", e);
        }
        if (!response.has("content") || !response.get("content").isArray()) {
            throw new AiGenerationException("Claude API response has no content array.");
        }
        for (JsonNode block : response.get("content")) {
            if ("text".equals(block.path("type").asText()) && StringUtils.hasText(block.path("text").asText())) {
                return block.path("text").asText();
            }
        }
        throw new AiGenerationException("Claude API response has no text content.");
    }

    private List<RawMenuCandidate> readRawMenuCandidates(String text) {
        log.debug("Claude menu raw text: {}", text);
        try {
            JsonNode root = objectMapper.readTree(extractJsonPayload(text));
            JsonNode candidatesNode = root.isArray() ? root : root.path("candidates");
            if (!candidatesNode.isArray()) {
                throw new AiGenerationException("Claude menu response must be an array or contain candidates array.");
            }
            return normalizeRawMenuCandidates(candidatesNode);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("Claude menu parse failed: {}", e.getMessage());
            throw new AiGenerationException("Claude menu response cannot be parsed: " + e.getMessage(), e);
        }
    }

    private List<RawMenuCandidate> normalizeRawMenuCandidates(JsonNode candidatesNode) {
        List<RawMenuCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < candidatesNode.size() && i < 6; i++) {
            JsonNode node = candidatesNode.get(i);
            String menuType = normalizeMenuType(text(node, "menuType", "menu_type", "type"), i);
            candidates.add(new RawMenuCandidate(
                    defaultText(text(node, "menuName", "menu_name", "name", "title"), "AI 추천 메뉴"),
                    menuType,
                    readUsedIngredients(node),
                    readStringList(node, "commonKitItems", "common_kit_items", "kitItems", "kit_items"),
                    readPurchaseItems(node),
                    intValue(node, 35, "cookingTimeMinutes", "cooking_time_minutes", "timeMinutes"),
                    defaultText(text(node, "difficulty", "difficultyLevel", "difficulty_level"), "LOW"),
                    defaultText(text(node, "recommendationReason", "recommendation_reason", "reason"), "남은 재료를 활용하기 좋은 후보입니다."),
                    defaultList(readStringList(node, "cookingOutlineSteps", "cooking_outline_steps", "steps"), List.of("재료 손질", "조리", "마무리")),
                    defaultList(readStringList(node, "rolePlanSummary", "role_plan_summary", "roles"), List.of("손질", "조리", "정리"))
            ));
        }
        return candidates;
    }

    private List<CandidateUsedIngredientResponse> readUsedIngredients(JsonNode node) {
        JsonNode array = array(node, "usedLeftoverIngredients", "used_leftover_ingredients", "leftoverIngredients");
        if (!array.isArray()) {
            return List.of();
        }

        List<CandidateUsedIngredientResponse> ingredients = new ArrayList<>();
        for (JsonNode item : array) {
            Long ingredientId = longValue(item, null, "ingredientId", "ingredient_id", "id");
            if (ingredientId == null) {
                continue;
            }
            BigDecimal availableGrams = decimalValue(item, BigDecimal.ZERO, "availableGrams", "available_grams");
            BigDecimal plannedUseGrams = decimalValue(item, availableGrams, "plannedUseGrams", "planned_use_grams", "useGrams");
            BigDecimal estimatedUseRatio = decimalValue(item, null, "estimatedUseRatio", "estimated_use_ratio", "useRatio");
            if (estimatedUseRatio == null) {
                estimatedUseRatio = calculateUseRatio(availableGrams, plannedUseGrams);
            }
            ingredients.add(new CandidateUsedIngredientResponse(
                    ingredientId,
                    defaultText(text(item, "nameKo", "name_ko", "name"), "재료 " + ingredientId),
                    availableGrams,
                    plannedUseGrams,
                    estimatedUseRatio
            ));
        }
        return ingredients;
    }

    private List<PurchaseItemResponse> readPurchaseItems(JsonNode node) {
        JsonNode array = array(node, "purchaseItems", "purchase_items");
        if (!array.isArray()) {
            return List.of();
        }

        List<PurchaseItemResponse> items = new ArrayList<>();
        for (JsonNode item : array) {
            items.add(new PurchaseItemResponse(
                    defaultText(text(item, "name", "nameKo", "name_ko"), "추가구매"),
                    defaultText(text(item, "category"), "OTHER"),
                    decimalValue(item, BigDecimal.ZERO, "quantityGrams", "quantity_grams", "grams"),
                    readStringList(item, "allergenTags", "allergen_tags"),
                    text(item, "assignedToNickname", "assigned_to_nickname", "purchaser"),
                    intValue(item, 0, "estimatedCost", "estimated_cost", "cost")
            ));
        }
        return items;
    }

    private String normalizeMenuType(String value, int index) {
        if (StringUtils.hasText(value)) {
            String normalized = value.trim().toUpperCase();
            if (normalized.equals("GENERAL") || normalized.equals("LOW_CARBON")) {
                return normalized;
            }
        }
        return index < 3 ? "GENERAL" : "LOW_CARBON";
    }

    private BigDecimal calculateUseRatio(BigDecimal availableGrams, BigDecimal plannedUseGrams) {
        if (availableGrams == null || availableGrams.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return plannedUseGrams.divide(availableGrams, 4, RoundingMode.HALF_UP);
    }

    private JsonNode array(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isArray()) {
                return value;
            }
        }
        return objectMapper.createArrayNode();
    }

    private List<String> readStringList(JsonNode node, String... fieldNames) {
        JsonNode array = array(node, fieldNames);
        if (!array.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            if (StringUtils.hasText(item.asText())) {
                values.add(item.asText().trim());
            }
        }
        return values;
    }

    private String text(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private Long longValue(JsonNode node, Long defaultValue, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isIntegralNumber()) {
                return value.longValue();
            }
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                try {
                    return Long.parseLong(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private int intValue(JsonNode node, int defaultValue, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isNumber()) {
                return value.intValue();
            }
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                try {
                    return Integer.parseInt(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private BigDecimal decimalValue(JsonNode node, BigDecimal defaultValue, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isNumber()) {
                return value.decimalValue();
            }
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                try {
                    return new BigDecimal(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private List<String> defaultList(List<String> value, List<String> defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
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
