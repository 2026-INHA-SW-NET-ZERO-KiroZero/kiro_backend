package com.kirozero.netzero.domain.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.ai.model.AiParticipant;
import com.kirozero.netzero.domain.ai.model.CookingGuideGenerationContext;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideStepResponse;
import com.kirozero.netzero.domain.cooking.dto.CookingUsedIngredientResponse;
import com.kirozero.netzero.domain.cooking.dto.ParticipantTaskResponse;
import com.kirozero.netzero.domain.recommendation.dto.CandidateUsedIngredientResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class CookingGuideResponseParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CookingGuideResponseParser() {
    }

    static CookingGuideResponse parseOrFallback(String text, CookingGuideGenerationContext context) {
        try {
            JsonNode root = readRoot(text);
            JsonNode guideNode = unwrapGuideNode(root);
            List<CookingGuideStepResponse> steps = readSteps(guideNode, context);
            if (steps.isEmpty()) {
                return fallbackGuide(context);
            }
            return new CookingGuideResponse(
                    context.slotId(),
                    context.selectedMenu().menuName(),
                    steps
            );
        } catch (RuntimeException e) {
            return fallbackGuide(context);
        }
    }

    private static JsonNode readRoot(String text) {
        String payload = extractJsonPayload(text);
        try {
            return OBJECT_MAPPER.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cooking guide JSON cannot be parsed.", e);
        }
    }

    private static JsonNode unwrapGuideNode(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        for (String fieldName : List.of("cookingGuide", "cooking_guide", "guide", "result", "data")) {
            JsonNode value = root.path(fieldName);
            if (value.isObject() || value.isArray()) {
                return value;
            }
        }
        return root;
    }

    private static List<CookingGuideStepResponse> readSteps(
            JsonNode guideNode,
            CookingGuideGenerationContext context
    ) {
        JsonNode stepsNode = guideNode.isArray()
                ? guideNode
                : array(guideNode, "steps", "cookingSteps", "cooking_steps", "recipeSteps", "recipe_steps");
        if (!stepsNode.isArray()) {
            return List.of();
        }

        List<CookingGuideStepResponse> steps = new ArrayList<>();
        for (int i = 0; i < stepsNode.size(); i++) {
            steps.add(readStep(stepsNode.get(i), i, context, stepsNode.size()));
        }
        return steps;
    }

    private static CookingGuideStepResponse readStep(
            JsonNode node,
            int index,
            CookingGuideGenerationContext context,
            int totalSteps
    ) {
        List<CookingUsedIngredientResponse> usedIngredients = readUsedIngredients(node, context);
        String phase = defaultText(text(node, "phase", "stepPhase", "step_phase"), defaultPhase(index, totalSteps)).toUpperCase();
        String title = defaultText(text(node, "title", "stepTitle", "step_title"), defaultStepTitle(index, phase, context));
        String instruction = defaultText(
                text(node, "instruction", "description", "displayInstruction", "display_instruction"),
                title + " 단계입니다. 재료와 도구를 확인하고 각자 맡은 역할대로 진행합니다."
        );

        return new CookingGuideStepResponse(
                intValue(node, index + 1, "stepOrder", "step_order", "order", "index"),
                phase,
                title,
                intValue(node, defaultMinutes(index, totalSteps, context), "estimatedMinutes", "estimated_minutes", "minutes"),
                instruction,
                usedIngredients,
                stringList(node, "tools", "requiredTools", "required_tools"),
                defaultStringList(stringList(node, "kitItems", "kit_items", "commonKitItems", "common_kit_items"), context.selectedMenu().commonKitItems()),
                readParticipantTasks(node, context, usedIngredients, phase, title),
                defaultText(text(node, "safetyNote", "safety_note"), "뜨거운 도구와 칼을 사용할 때는 주변 사람에게 먼저 알리고 천천히 움직입니다."),
                defaultText(text(node, "completionCriteria", "completion_criteria"), title + " 단계가 끝나면 다음 단계로 이동합니다.")
        );
    }

    private static List<CookingUsedIngredientResponse> readUsedIngredients(
            JsonNode node,
            CookingGuideGenerationContext context
    ) {
        JsonNode array = array(node, "usedIngredients", "used_ingredients", "ingredients", "usedLeftoverIngredients");
        List<CookingUsedIngredientResponse> ingredients = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode item : array) {
                Long ingredientId = longValue(item, null, "ingredientId", "ingredient_id", "id");
                if (ingredientId == null) {
                    continue;
                }
                ingredients.add(new CookingUsedIngredientResponse(
                        ingredientId,
                        defaultText(text(item, "nameKo", "name_ko", "name"), findIngredientName(context, ingredientId)),
                        decimalValue(item, findPlannedUseGrams(context, ingredientId), "plannedUseGrams", "planned_use_grams", "grams")
                ));
            }
        }
        if (!ingredients.isEmpty()) {
            return ingredients;
        }
        return fallbackUsedIngredients(context);
    }

    private static List<ParticipantTaskResponse> readParticipantTasks(
            JsonNode node,
            CookingGuideGenerationContext context,
            List<CookingUsedIngredientResponse> usedIngredients,
            String phase,
            String title
    ) {
        JsonNode array = array(node, "participantTasks", "participant_tasks", "tasks", "roleAssignments", "role_assignments");
        Map<Long, ParticipantTaskResponse> byParticipant = new LinkedHashMap<>();
        if (array.isArray()) {
            for (JsonNode item : array) {
                AiParticipant participant = findParticipant(item, context);
                if (participant == null) {
                    continue;
                }
                byParticipant.putIfAbsent(
                        participant.participantId(),
                        readParticipantTask(item, participant, usedIngredients, phase, title)
                );
            }
        }

        List<ParticipantTaskResponse> tasks = new ArrayList<>();
        int index = 0;
        for (AiParticipant participant : context.participants()) {
            ParticipantTaskResponse task = byParticipant.get(participant.participantId());
            if (task == null) {
                task = fallbackTask(participant, usedIngredients, phase, title, index);
            }
            tasks.add(task);
            index += 1;
        }
        return tasks;
    }

    private static ParticipantTaskResponse readParticipantTask(
            JsonNode item,
            AiParticipant participant,
            List<CookingUsedIngredientResponse> usedIngredients,
            String phase,
            String title
    ) {
        CookingUsedIngredientResponse ingredient = pickIngredient(usedIngredients, 0);
        String defaultTaskName = title + " 담당";
        String defaultPurpose = "조리 흐름이 끊기지 않게 " + title + " 단계를 맡기 위해";
        String defaultDetail = detailFor(phase, ingredient, title);
        String taskName = defaultText(text(item, "taskName", "task_name", "role", "name"), defaultTaskName);
        String taskPurpose = defaultText(text(item, "taskPurpose", "task_purpose", "purpose"), defaultPurpose);
        String taskDetail = defaultText(text(item, "taskDetail", "task_detail", "detail"), defaultDetail);
        return new ParticipantTaskResponse(
                participant.participantId(),
                participant.nickname(),
                taskName,
                taskPurpose,
                taskDetail,
                defaultStringList(
                        stringList(item, "attentionPoints", "attention_points", "cautions"),
                        List.of("도구를 사용하기 전에 주변을 확인합니다.", "뜨거운 조리도구는 맨손으로 잡지 않습니다.")
                ),
                defaultText(
                        text(item, "displayInstruction", "display_instruction"),
                        taskPurpose + ", " + taskDetail
                ),
                defaultText(text(item, "skillRequired", "skill_required"), participant.cookingSkill())
        );
    }

    private static CookingGuideResponse fallbackGuide(CookingGuideGenerationContext context) {
        List<CookingUsedIngredientResponse> usedIngredients = fallbackUsedIngredients(context);
        return new CookingGuideResponse(
                context.slotId(),
                context.selectedMenu().menuName(),
                List.of(
                        fallbackStep(1, "PREP", "재료 확인과 손질", 10, usedIngredients, context),
                        fallbackStep(2, "COOK", "메인 조리", Math.max(15, context.selectedMenu().cookingTimeMinutes()), usedIngredients, context),
                        fallbackStep(3, "FINISH", "담기와 기록", 8, List.of(), context)
                )
        );
    }

    private static CookingGuideStepResponse fallbackStep(
            int order,
            String phase,
            String title,
            int minutes,
            List<CookingUsedIngredientResponse> usedIngredients,
            CookingGuideGenerationContext context
    ) {
        List<ParticipantTaskResponse> tasks = new ArrayList<>();
        for (int i = 0; i < context.participants().size(); i++) {
            tasks.add(fallbackTask(context.participants().get(i), usedIngredients, phase, title, i));
        }
        return new CookingGuideStepResponse(
                order,
                phase,
                title,
                minutes,
                fallbackStepInstruction(phase, title, usedIngredients, context),
                usedIngredients,
                defaultTools(phase),
                "FINISH".equals(phase) ? List.of() : context.selectedMenu().commonKitItems(),
                tasks,
                "칼과 뜨거운 팬을 사용할 때는 한 번에 한 사람만 접근합니다.",
                title + " 단계가 끝나면 다음 단계로 이동할 수 있습니다."
        );
    }

    private static ParticipantTaskResponse fallbackTask(
            AiParticipant participant,
            List<CookingUsedIngredientResponse> usedIngredients,
            String phase,
            String title,
            int index
    ) {
        CookingUsedIngredientResponse ingredient = pickIngredient(usedIngredients, index);
        String taskPurpose = fallbackPurpose(phase, title);
        String taskDetail = detailFor(phase, ingredient, title, index, usedIngredients.size());
        return new ParticipantTaskResponse(
                participant.participantId(),
                participant.nickname(),
                fallbackTaskName(phase, ingredient, title, index, usedIngredients.size()),
                taskPurpose,
                taskDetail,
                attentionPointsFor(phase),
                taskPurpose + ", " + taskDetail,
                participant.cookingSkill()
        );
    }

    private static String detailFor(String phase, CookingUsedIngredientResponse ingredient, String title) {
        return detailFor(phase, ingredient, title, 0, ingredient == null ? 0 : 1);
    }

    private static String detailFor(
            String phase,
            CookingUsedIngredientResponse ingredient,
            String title,
            int index,
            int usedIngredientCount
    ) {
        String supportDetail = supportDetailFor(phase, title, index, usedIngredientCount);
        if (supportDetail != null) {
            return supportDetail;
        }
        if (ingredient == null) {
            if ("FINISH".equals(phase)) {
                return "완성 사진을 먼저 찍고, 사용한 재료의 소진량을 0/25/50/75/100% 중 하나로 기록합니다.";
            }
            return title + "에 필요한 도구를 준비하고 조리대 주변을 정리합니다.";
        }
        String grams = ingredient.plannedUseGrams().stripTrailingZeros().toPlainString();
        if ("PREP".equals(phase)) {
            return ingredient.nameKo() + " " + grams + "g을 씻고 물기를 턴 뒤 0.5cm 정도 두께로 썹니다. "
                    + "익는 속도가 맞도록 너무 큰 조각은 한 번 더 잘라 둡니다.";
        }
        if ("COOK".equals(phase)) {
            return "중불로 예열한 팬에 " + ingredient.nameKo() + " " + grams
                    + "g을 넣고 3분 정도 먼저 익힙니다. 가장자리가 투명해지거나 겉면 색이 바뀌면 다음 재료를 넣을 준비를 합니다.";
        }
        return ingredient.nameKo() + " " + grams + "g이 실제로 얼마나 소진됐는지 확인하고, 완성 사진과 소진량 기록을 준비합니다.";
    }

    private static String fallbackStepInstruction(
            String phase,
            String title,
            List<CookingUsedIngredientResponse> usedIngredients,
            CookingGuideGenerationContext context
    ) {
        String summary = ingredientSummary(usedIngredients);
        if ("PREP".equals(phase)) {
            return title + " 단계입니다. " + summary
                    + "를 바로 조리할 수 있게 씻고 물기를 제거한 뒤, 0.5cm 정도로 일정하게 손질합니다.";
        }
        if ("COOK".equals(phase)) {
            String kit = context.selectedMenu().commonKitItems().isEmpty()
                    ? "기본 조미료"
                    : String.join(", ", context.selectedMenu().commonKitItems());
            return title + " 단계입니다. 중불로 팬을 예열하고 " + kit
                    + "를 사용해 향이 나는 재료부터 3분 정도 볶은 뒤, 남은 재료를 순서대로 넣어 익힘 상태를 확인합니다.";
        }
        return title + " 단계입니다. 완성 음식 사진을 찍고, 남은 식재료 소진량과 식후 남은 음식 여부를 기록합니다.";
    }

    private static String fallbackTaskName(
            String phase,
            CookingUsedIngredientResponse ingredient,
            String title,
            int index,
            int usedIngredientCount
    ) {
        if ("FINISH".equals(phase)) {
            return switch (index % 4) {
                case 0 -> "완성 사진 기록";
                case 1 -> "소진량 체크";
                case 2 -> "식후 사진 기록";
                default -> "정리와 안전 확인";
            };
        }
        String supportTaskName = supportTaskNameFor(phase, index, usedIngredientCount);
        if (supportTaskName != null) {
            return supportTaskName;
        }
        if (ingredient != null) {
            if ("PREP".equals(phase)) {
                return ingredient.nameKo() + " 손질";
            }
            if ("COOK".equals(phase)) {
                return ingredient.nameKo() + " 익힘 확인";
            }
        }
        return title + " 담당";
    }

    private static String supportTaskNameFor(String phase, int index, int usedIngredientCount) {
        if (usedIngredientCount <= 0 || index < usedIngredientCount) {
            return null;
        }
        int supportIndex = (index - usedIngredientCount) % 3;
        if ("PREP".equals(phase)) {
            return switch (supportIndex) {
                case 0 -> "도구와 볼 준비";
                case 1 -> "물기 제거와 재료 정리";
                default -> "손질 상태 검수";
            };
        }
        if ("COOK".equals(phase)) {
            return switch (supportIndex) {
                case 0 -> "불 조절과 팬 확인";
                case 1 -> "양념 계량";
                default -> "조리 도구 정리";
            };
        }
        return null;
    }

    private static String supportDetailFor(String phase, String title, int index, int usedIngredientCount) {
        String taskName = supportTaskNameFor(phase, index, usedIngredientCount);
        if (taskName == null) {
            return null;
        }
        return switch (taskName) {
            case "도구와 볼 준비" -> title + "에 필요한 도마, 칼, 볼을 준비하고 손질 전 재료와 손질 후 재료를 담을 공간을 나눕니다.";
            case "물기 제거와 재료 정리" -> "씻은 재료의 물기를 체나 키친타월로 제거하고, 손질된 재료를 조리 순서대로 볼에 나눠 담습니다.";
            case "손질 상태 검수" -> "재료 크기가 너무 큰 조각은 다시 자르고, 껍질이나 심지처럼 먹기 어려운 부분이 남았는지 확인합니다.";
            case "불 조절과 팬 확인" -> "팬을 중불로 예열하고 재료가 타지 않도록 불 세기를 확인합니다. 연기가 나면 바로 불을 낮춥니다.";
            case "양념 계량" -> "공용 양념을 한 번에 붓지 않도록 작은 그릇에 덜어 두고, 간장과 소금은 조금씩 넣을 수 있게 준비합니다.";
            case "조리 도구 정리" -> "사용한 칼과 도마를 조리대 가장자리에서 치우고, 팬 주변에 물기나 장애물이 없도록 정리합니다.";
            default -> null;
        };
    }

    private static String fallbackPurpose(String phase, String title) {
        if ("PREP".equals(phase)) {
            return "재료가 동시에 익도록 크기와 두께를 맞추기 위해";
        }
        if ("COOK".equals(phase)) {
            return "팬 안에서 재료가 타지 않고 순서대로 익도록 조리 흐름을 나누기 위해";
        }
        return "성과 리포트에 필요한 사진과 소진량 데이터를 남기기 위해";
    }

    private static List<String> attentionPointsFor(String phase) {
        if ("PREP".equals(phase)) {
            return List.of("칼질 전 도마가 미끄러지지 않는지 확인합니다.", "손가락을 말아 쥐고 천천히 썹니다.");
        }
        if ("COOK".equals(phase)) {
            return List.of("팬은 중불에서 시작하고 연기가 나면 불을 낮춥니다.", "뜨거운 팬 근처에는 한 번에 한 사람만 접근합니다.");
        }
        return List.of("사진을 찍기 전에 음식이 잘 보이도록 한 접시에 모읍니다.", "소진량은 과장하지 않고 실제 사용량에 가깝게 기록합니다.");
    }

    private static String ingredientSummary(List<CookingUsedIngredientResponse> usedIngredients) {
        if (usedIngredients.isEmpty()) {
            return "선택된 재료";
        }
        return usedIngredients.stream()
                .map(ingredient -> ingredient.nameKo() + " " + ingredient.plannedUseGrams().stripTrailingZeros().toPlainString() + "g")
                .reduce((left, right) -> left + ", " + right)
                .orElse("선택된 재료");
    }

    private static AiParticipant findParticipant(JsonNode item, CookingGuideGenerationContext context) {
        Long participantId = longValue(item, null, "participantId", "participant_id", "id");
        if (participantId != null) {
            for (AiParticipant participant : context.participants()) {
                if (participant.participantId().equals(participantId)) {
                    return participant;
                }
            }
        }
        String nickname = text(item, "nickname", "name", "participantName", "participant_name");
        if (StringUtils.hasText(nickname)) {
            for (AiParticipant participant : context.participants()) {
                if (participant.nickname().equals(nickname)) {
                    return participant;
                }
            }
        }
        return null;
    }

    private static CookingUsedIngredientResponse pickIngredient(List<CookingUsedIngredientResponse> usedIngredients, int index) {
        if (usedIngredients.isEmpty()) {
            return null;
        }
        return usedIngredients.get(index % usedIngredients.size());
    }

    private static List<CookingUsedIngredientResponse> fallbackUsedIngredients(CookingGuideGenerationContext context) {
        return context.selectedMenu().usedLeftoverIngredients().stream()
                .map(ingredient -> new CookingUsedIngredientResponse(
                        ingredient.ingredientId(),
                        ingredient.nameKo(),
                        ingredient.plannedUseGrams()
                ))
                .toList();
    }

    private static String findIngredientName(CookingGuideGenerationContext context, Long ingredientId) {
        return context.selectedMenu().usedLeftoverIngredients().stream()
                .filter(ingredient -> ingredient.ingredientId().equals(ingredientId))
                .findFirst()
                .map(CandidateUsedIngredientResponse::nameKo)
                .orElse("재료 " + ingredientId);
    }

    private static BigDecimal findPlannedUseGrams(CookingGuideGenerationContext context, Long ingredientId) {
        return context.selectedMenu().usedLeftoverIngredients().stream()
                .filter(ingredient -> ingredient.ingredientId().equals(ingredientId))
                .findFirst()
                .map(CandidateUsedIngredientResponse::plannedUseGrams)
                .orElse(BigDecimal.ZERO);
    }

    private static String defaultPhase(int index, int totalSteps) {
        if (index == 0) {
            return "PREP";
        }
        if (index == totalSteps - 1) {
            return "FINISH";
        }
        return "COOK";
    }

    private static String defaultStepTitle(int index, String phase, CookingGuideGenerationContext context) {
        List<String> outline = context.selectedMenu().cookingOutlineSteps();
        if (outline != null && index < outline.size() && StringUtils.hasText(outline.get(index))) {
            return outline.get(index);
        }
        return switch (phase) {
            case "PREP" -> "재료 손질";
            case "FINISH" -> "담기와 기록";
            default -> "메인 조리";
        };
    }

    private static int defaultMinutes(int index, int totalSteps, CookingGuideGenerationContext context) {
        if (totalSteps <= 0) {
            return 10;
        }
        return Math.max(5, context.selectedMenu().cookingTimeMinutes() / Math.max(1, totalSteps));
    }

    private static List<String> defaultTools(String phase) {
        return switch (phase) {
            case "PREP" -> List.of("도마", "칼", "볼");
            case "FINISH" -> List.of("접시", "집게");
            default -> List.of("프라이팬", "주걱", "냄비");
        };
    }

    private static JsonNode array(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isArray()) {
                return value;
            }
        }
        return OBJECT_MAPPER.createArrayNode();
    }

    private static List<String> stringList(JsonNode node, String... fieldNames) {
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

    private static List<String> defaultStringList(List<String> value, List<String> defaultValue) {
        return value == null || value.isEmpty() ? safeList(defaultValue) : value;
    }

    private static String text(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private static Long longValue(JsonNode node, Long defaultValue, String... fieldNames) {
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

    private static int intValue(JsonNode node, int defaultValue, String... fieldNames) {
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

    private static BigDecimal decimalValue(JsonNode node, BigDecimal defaultValue, String... fieldNames) {
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
        return defaultValue == null ? BigDecimal.ZERO : defaultValue;
    }

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private static String extractJsonPayload(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("Cooking guide response is blank.");
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
            throw new IllegalArgumentException("Cooking guide response does not contain JSON.");
        }
        return cleaned.substring(start, end + 1);
    }
}
