package com.kirozero.netzero.domain.ai.service;

import com.kirozero.netzero.domain.ai.enums.AiProvider;
import com.kirozero.netzero.domain.ai.model.AiIngredient;
import com.kirozero.netzero.domain.ai.model.AiParticipant;
import com.kirozero.netzero.domain.ai.model.CookingGuideGenerationContext;
import com.kirozero.netzero.domain.ai.model.MenuCandidateGenerationContext;
import com.kirozero.netzero.domain.ai.model.RawMenuCandidate;
import com.kirozero.netzero.domain.ai.port.AiGenerationAdapter;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideStepResponse;
import com.kirozero.netzero.domain.cooking.dto.CookingUsedIngredientResponse;
import com.kirozero.netzero.domain.cooking.dto.ParticipantTaskResponse;
import com.kirozero.netzero.domain.recommendation.dto.CandidateUsedIngredientResponse;
import com.kirozero.netzero.domain.recommendation.dto.PurchaseItemResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StubAiGenerationAdapter implements AiGenerationAdapter {

    private static final BigDecimal DEFAULT_USE_RATIO = new BigDecimal("0.80");

    @Override
    public AiProvider provider() {
        return AiProvider.STUB;
    }

    @Override
    public List<RawMenuCandidate> generateMenuCandidates(MenuCandidateGenerationContext context) {
        List<AiIngredient> orderedIngredients = context.sharedIngredients().stream()
                .sorted(Comparator.comparing(AiIngredient::availableGrams).reversed())
                .toList();
        List<CandidateUsedIngredientResponse> mainUses = toCandidateUses(orderedIngredients);
        boolean hasPurchaser = context.participants().stream().anyMatch(AiParticipant::canPurchase);

        return List.of(
                new RawMenuCandidate(
                        "냉장고 채소 볶음밥",
                        "GENERAL",
                        mainUses,
                        List.of("식용유", "간장", "후추", "참기름"),
                        List.of(),
                        35,
                        "LOW",
                        "남은 재료를 잘게 썰어 한 번에 볶을 수 있어 소진율이 높습니다.",
                        List.of("재료를 비슷한 크기로 손질합니다.", "밥과 함께 볶고 간장으로 간을 맞춥니다.", "참기름으로 마무리합니다."),
                        List.of("손질", "볶기", "간 맞춤", "플레이팅")
                ),
                new RawMenuCandidate(
                        "감자 채소전",
                        "GENERAL",
                        mainUses,
                        List.of("식용유", "소금", "후추"),
                        hasPurchaser ? eggPurchaseItems() : List.of(),
                        40,
                        "MEDIUM",
                        "채소와 전분감 있는 재료를 묶어 조리하기 쉬운 일반식 후보입니다.",
                        List.of("재료를 얇게 채 썹니다.", "반죽 농도를 맞춥니다.", "앞뒤로 노릇하게 굽습니다."),
                        List.of("채썰기", "반죽", "부치기", "정리")
                ),
                new RawMenuCandidate(
                        "닭가슴살 채소 덮밥",
                        "GENERAL",
                        mainUses,
                        List.of("식용유", "간장", "후추"),
                        hasPurchaser ? chickenPurchaseItems() : List.of(),
                        45,
                        "MEDIUM",
                        "남은 채소에 닭가슴살을 더해 든든한 일반식을 제안합니다.",
                        List.of("닭가슴살을 한입 크기로 자릅니다.", "채소와 함께 볶습니다.", "덮밥으로 마무리합니다."),
                        List.of("손질", "볶기", "간 맞춤", "담기")
                ),
                new RawMenuCandidate(
                        "저탄소 채소 비빔밥",
                        "LOW_CARBON",
                        mainUses,
                        List.of("간장", "참기름", "소금"),
                        List.of(),
                        30,
                        "LOW",
                        "추가 고기류 없이 남은 채소 중심으로 구성해 저탄소 선택에 적합합니다.",
                        List.of("재료를 데치거나 볶습니다.", "밥 위에 재료를 올립니다.", "간장 양념으로 비빕니다."),
                        List.of("데치기", "볶기", "양념", "담기")
                ),
                new RawMenuCandidate(
                        "저탄소 채소 덮밥",
                        "LOW_CARBON",
                        mainUses,
                        List.of("식용유", "간장", "후추"),
                        List.of(),
                        35,
                        "LOW",
                        "공용 키트만으로 조리 가능하고 추가구매 부담이 낮은 후보입니다.",
                        List.of("재료를 한입 크기로 썹니다.", "센 불에 빠르게 볶습니다.", "밥 위에 얹어 마무리합니다."),
                        List.of("손질", "볶기", "밥 준비", "마무리")
                ),
                new RawMenuCandidate(
                        "두부 채소 볶음",
                        "LOW_CARBON",
                        mainUses,
                        List.of("식용유", "간장", "참기름"),
                        List.of(),
                        30,
                        "LOW",
                        "두부와 남은 채소를 활용해 단백질과 저탄소를 함께 챙기는 후보입니다.",
                        List.of("두부 물기를 제거합니다.", "채소와 함께 볶습니다.", "간장과 참기름으로 마무리합니다."),
                        List.of("두부 손질", "채소 손질", "볶기", "정리")
                )
        );
    }

    @Override
    public CookingGuideResponse generateCookingGuide(CookingGuideGenerationContext context) {
        List<CookingUsedIngredientResponse> usedIngredients = context.selectedMenu().usedLeftoverIngredients().stream()
                .map(ingredient -> new CookingUsedIngredientResponse(
                        ingredient.ingredientId(),
                        ingredient.nameKo(),
                        ingredient.plannedUseGrams()
                ))
                .toList();

        return new CookingGuideResponse(
                context.slotId(),
                context.selectedMenu().menuName(),
                List.of(
                        new CookingGuideStepResponse(
                                1,
                                "PREP",
                                "재료 확인과 손질",
                                10,
                                context.selectedMenu().menuName() + "에 사용할 남은 재료를 확인하고, 조리하기 좋은 크기로 손질합니다.",
                                usedIngredients,
                                List.of("도마", "칼", "볼"),
                                context.selectedMenu().commonKitItems(),
                                buildPrepTasks(context.participants(), usedIngredients),
                                "칼 사용자는 도마 주변을 비우고, 손질한 재료와 씻기 전 재료를 섞지 않습니다.",
                                "모든 재료가 조리 순서에 맞게 분리되어 있음"
                        ),
                        new CookingGuideStepResponse(
                                2,
                                "COOK",
                                "메인 조리",
                                context.selectedMenu().cookingTimeMinutes(),
                                "손질한 재료를 순서대로 익히고 공용 키트로 간을 맞춥니다.",
                                usedIngredients,
                                List.of("프라이팬", "주걱", "냄비"),
                                context.selectedMenu().commonKitItems(),
                                buildCookTasks(context.participants(), context.selectedMenu().menuName()),
                                "뜨거운 팬 주변에는 한 사람만 서고, 양념은 조금씩 넣어 간을 확인합니다.",
                                "재료가 골고루 익고 메뉴의 기본 간이 맞음"
                        ),
                        new CookingGuideStepResponse(
                                3,
                                "FINISH",
                                "담기와 기록",
                                8,
                                "완성 음식을 나눠 담고, 결과 제출에 사용할 완성 사진을 촬영합니다.",
                                List.of(),
                                List.of("접시", "집게"),
                                List.of(),
                                buildFinishTasks(context.participants()),
                                "사진 촬영 전 조리대 주변 위험한 도구를 정리합니다.",
                                "완성 음식 사진이 준비되고 식사 전 정리가 끝남"
                        )
                )
        );
    }

    private List<CandidateUsedIngredientResponse> toCandidateUses(List<AiIngredient> ingredients) {
        return ingredients.stream()
                .limit(5)
                .map(item -> {
                    BigDecimal plannedUseGrams = item.availableGrams()
                            .multiply(DEFAULT_USE_RATIO)
                            .setScale(1, RoundingMode.HALF_UP);
                    return new CandidateUsedIngredientResponse(
                            item.ingredientId(),
                            item.nameKo(),
                            item.availableGrams(),
                            plannedUseGrams,
                            DEFAULT_USE_RATIO
                    );
                })
                .toList();
    }

    private List<PurchaseItemResponse> eggPurchaseItems() {
        return List.of(new PurchaseItemResponse(
                "계란",
                "EGG",
                new BigDecimal("240"),
                List.of("egg"),
                null,
                3000
        ));
    }

    private List<PurchaseItemResponse> chickenPurchaseItems() {
        return List.of(new PurchaseItemResponse(
                "닭가슴살",
                "POULTRY",
                new BigDecimal("400"),
                List.of(),
                null,
                6000
        ));
    }

    private List<ParticipantTaskResponse> buildPrepTasks(
            List<AiParticipant> participants,
            List<CookingUsedIngredientResponse> usedIngredients
    ) {
        return participants.stream()
                .map(participant -> {
                    String ingredientText = usedIngredients.isEmpty()
                            ? "담당 재료"
                            : usedIngredients.get(participants.indexOf(participant) % usedIngredients.size()).nameKo();
                    return new ParticipantTaskResponse(
                            participant.participantId(),
                            participant.nickname(),
                            ingredientText + " 손질",
                            "조리 단계에서 재료가 고르게 익도록 준비하기 위해",
                            ingredientText + "을 씻고 먹기 좋은 크기로 자릅니다.",
                            List.of("손질한 재료는 깨끗한 볼에 따로 둡니다.", "물기가 많으면 볶을 때 질척해질 수 있으니 가볍게 털어냅니다."),
                            "조리 단계에서 재료가 고르게 익도록, " + ingredientText + "을 씻고 먹기 좋은 크기로 자릅니다. 손질한 재료는 깨끗한 볼에 따로 둬 주세요.",
                            participant.cookingSkill()
                    );
                })
                .toList();
    }

    private List<ParticipantTaskResponse> buildCookTasks(
            List<AiParticipant> participants,
            String menuName
    ) {
        return participants.stream()
                .map(participant -> new ParticipantTaskResponse(
                        participant.participantId(),
                        participant.nickname(),
                        menuName + " 조리 보조",
                        "조리 흐름이 끊기지 않게 손질 재료와 공용 키트를 순서대로 전달하기 위해",
                        "메인 조리자 옆에서 재료와 양념을 순서대로 전달하고, 필요한 도구를 정리합니다.",
                        List.of("뜨거운 팬에는 손을 가까이 대지 않습니다.", "양념은 한 번에 많이 넣지 말고 조금씩 전달합니다."),
                        "조리 흐름이 끊기지 않도록, 재료와 양념을 순서대로 전달하고 필요한 도구를 바로 정리합니다. 뜨거운 팬에는 손을 가까이 대지 마세요.",
                        participant.cookingSkill()
                ))
                .toList();
    }

    private List<ParticipantTaskResponse> buildFinishTasks(List<AiParticipant> participants) {
        return participants.stream()
                .map(participant -> new ParticipantTaskResponse(
                        participant.participantId(),
                        participant.nickname(),
                        "담기와 정리",
                        "결과 제출과 식사 진행이 자연스럽게 이어지도록 마무리하기 위해",
                        "완성 음식을 나눠 담고, 조리대 주변의 사용한 도구를 한곳에 모읍니다.",
                        List.of("사진에 완성 음식이 잘 보이도록 흔들림 없이 촬영합니다.", "뜨거운 도구는 바로 손으로 잡지 않습니다."),
                        "결과 제출과 식사 진행이 자연스럽게 이어지도록, 완성 음식을 나눠 담고 사용한 도구를 한곳에 모읍니다.",
                        participant.cookingSkill()
                ))
                .toList();
    }
}
