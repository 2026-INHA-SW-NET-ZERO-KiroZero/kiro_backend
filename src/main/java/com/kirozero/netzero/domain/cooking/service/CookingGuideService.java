package com.kirozero.netzero.domain.cooking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideResponse;
import com.kirozero.netzero.domain.cooking.dto.CookingGuideStepResponse;
import com.kirozero.netzero.domain.cooking.dto.CookingUsedIngredientResponse;
import com.kirozero.netzero.domain.cooking.dto.ParticipantTaskResponse;
import com.kirozero.netzero.domain.recommendation.dto.MenuCandidateResponse;
import com.kirozero.netzero.domain.session.entity.SessionParticipant;
import com.kirozero.netzero.domain.session.repository.SessionParticipantRepository;
import com.kirozero.netzero.domain.slot.entity.Slot;
import com.kirozero.netzero.domain.slot.repository.SlotRepository;
import com.kirozero.netzero.domain.user.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CookingGuideService {

    private final AuthService authService;
    private final SlotRepository slotRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public CookingGuideResponse getCookingGuide(Long slotId, String authorizationHeader, String view) {
        User user = authService.requireUser(authorizationHeader);
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        SessionParticipant currentParticipant = sessionParticipantRepository.findBySlotIdAndUserId(slotId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Only participants can view cooking guide."));
        MenuCandidateResponse selectedMenu = readSelectedMenu(slot.getSelectedMenuJson());

        CookingGuideResponse guide = readOrCreateGuide(slot, selectedMenu);
        if (view == null || view.isBlank() || "all".equalsIgnoreCase(view)) {
            return guide;
        }
        if ("mine".equalsIgnoreCase(view)) {
            return filterMine(guide, currentParticipant.getId());
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "view must be all or mine.");
    }

    private CookingGuideResponse readOrCreateGuide(Slot slot, MenuCandidateResponse selectedMenu) {
        if (StringUtils.hasText(slot.getCookingPlanJson())) {
            try {
                return objectMapper.readValue(unwrapStoredJson(slot.getCookingPlanJson()), CookingGuideResponse.class);
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cooking guide cannot be parsed.", e);
            }
        }

        List<SessionParticipant> participants = sessionParticipantRepository.findBySlotIdOrderByJoinedAtAsc(slot.getId());
        CookingGuideResponse guide = buildGuide(slot.getId(), selectedMenu, participants);
        try {
            slot.saveCookingPlan(objectMapper.writeValueAsString(guide));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cooking guide cannot be serialized.", e);
        }
        return guide;
    }

    private CookingGuideResponse buildGuide(
            Long slotId,
            MenuCandidateResponse selectedMenu,
            List<SessionParticipant> participants
    ) {
        List<CookingUsedIngredientResponse> usedIngredients = selectedMenu.usedLeftoverIngredients().stream()
                .map(ingredient -> new CookingUsedIngredientResponse(
                        ingredient.ingredientId(),
                        ingredient.nameKo(),
                        ingredient.plannedUseGrams()
                ))
                .toList();

        return new CookingGuideResponse(
                slotId,
                selectedMenu.menuName(),
                List.of(
                        new CookingGuideStepResponse(
                                1,
                                "PREP",
                                "재료 확인과 손질",
                                10,
                                selectedMenu.menuName() + "에 사용할 남은 재료를 확인하고, 조리하기 좋은 크기로 손질합니다.",
                                usedIngredients,
                                List.of("도마", "칼", "볼"),
                                selectedMenu.commonKitItems(),
                                buildPrepTasks(participants, usedIngredients),
                                "칼 사용자는 도마 주변을 비우고, 손질한 재료와 씻기 전 재료를 섞지 않습니다.",
                                "모든 재료가 조리 순서에 맞게 분리되어 있음"
                        ),
                        new CookingGuideStepResponse(
                                2,
                                "COOK",
                                "메인 조리",
                                selectedMenu.cookingTimeMinutes(),
                                "손질한 재료를 순서대로 익히고 공용 키트로 간을 맞춥니다.",
                                usedIngredients,
                                List.of("프라이팬", "주걱", "냄비"),
                                selectedMenu.commonKitItems(),
                                buildCookTasks(participants, selectedMenu),
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
                                buildFinishTasks(participants),
                                "사진 촬영 전 조리대 주변 위험한 도구를 정리합니다.",
                                "완성 음식 사진이 준비되고 식사 전 정리가 끝남"
                        )
                )
        );
    }

    private List<ParticipantTaskResponse> buildPrepTasks(
            List<SessionParticipant> participants,
            List<CookingUsedIngredientResponse> usedIngredients
    ) {
        return participants.stream()
                .map(participant -> {
                    String ingredientText = usedIngredients.isEmpty()
                            ? "담당 재료"
                            : usedIngredients.get((participants.indexOf(participant)) % usedIngredients.size()).nameKo();
                    return new ParticipantTaskResponse(
                            participant.getId(),
                            participant.getUser().getNickname(),
                            ingredientText + " 손질",
                            "조리 단계에서 재료가 고르게 익도록 준비하기 위해",
                            ingredientText + "을 씻고 먹기 좋은 크기로 자릅니다.",
                            List.of("손질한 재료는 깨끗한 볼에 따로 둡니다.", "물기가 많으면 볶을 때 질척해질 수 있으니 가볍게 털어냅니다."),
                            "조리 단계에서 재료가 고르게 익도록, " + ingredientText + "을 씻고 먹기 좋은 크기로 자릅니다. 손질한 재료는 깨끗한 볼에 따로 둬 주세요.",
                            participant.getUser().getCookingSkill().name()
                    );
                })
                .toList();
    }

    private List<ParticipantTaskResponse> buildCookTasks(
            List<SessionParticipant> participants,
            MenuCandidateResponse selectedMenu
    ) {
        return participants.stream()
                .map(participant -> new ParticipantTaskResponse(
                        participant.getId(),
                        participant.getUser().getNickname(),
                        selectedMenu.menuName() + " 조리 보조",
                        "조리 흐름이 끊기지 않게 손질 재료와 공용 키트를 순서대로 전달하기 위해",
                        "메인 조리자 옆에서 재료와 양념을 순서대로 전달하고, 필요한 도구를 정리합니다.",
                        List.of("뜨거운 팬에는 손을 가까이 대지 않습니다.", "양념은 한 번에 많이 넣지 말고 조금씩 전달합니다."),
                        "조리 흐름이 끊기지 않도록, 재료와 양념을 순서대로 전달하고 필요한 도구를 바로 정리합니다. 뜨거운 팬에는 손을 가까이 대지 마세요.",
                        participant.getUser().getCookingSkill().name()
                ))
                .toList();
    }

    private List<ParticipantTaskResponse> buildFinishTasks(List<SessionParticipant> participants) {
        return participants.stream()
                .map(participant -> new ParticipantTaskResponse(
                        participant.getId(),
                        participant.getUser().getNickname(),
                        "담기와 정리",
                        "결과 제출과 식사 진행이 자연스럽게 이어지도록 마무리하기 위해",
                        "완성 음식을 나눠 담고, 조리대 주변의 사용한 도구를 한곳에 모읍니다.",
                        List.of("사진에 완성 음식이 잘 보이도록 흔들림 없이 촬영합니다.", "뜨거운 도구는 바로 손으로 잡지 않습니다."),
                        "결과 제출과 식사 진행이 자연스럽게 이어지도록, 완성 음식을 나눠 담고 사용한 도구를 한곳에 모읍니다.",
                        participant.getUser().getCookingSkill().name()
                ))
                .toList();
    }

    private CookingGuideResponse filterMine(CookingGuideResponse guide, Long participantId) {
        return new CookingGuideResponse(
                guide.slotId(),
                guide.menuName(),
                guide.steps().stream()
                        .map(step -> new CookingGuideStepResponse(
                                step.stepOrder(),
                                step.phase(),
                                step.title(),
                                step.estimatedMinutes(),
                                step.instruction(),
                                step.usedIngredients(),
                                step.tools(),
                                step.kitItems(),
                                step.participantTasks().stream()
                                        .filter(task -> task.participantId().equals(participantId))
                                        .toList(),
                                step.safetyNote(),
                                step.completionCriteria()
                        ))
                        .toList()
        );
    }

    private MenuCandidateResponse readSelectedMenu(String selectedMenuJson) {
        if (!StringUtils.hasText(selectedMenuJson)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected menu is required.");
        }

        try {
            return objectMapper.readValue(unwrapStoredJson(selectedMenuJson), MenuCandidateResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected menu cannot be parsed.", e);
        }
    }

    private String unwrapStoredJson(String value) throws JsonProcessingException {
        String unwrapped = value.trim();
        for (int i = 0; i < 3; i++) {
            if (!unwrapped.startsWith("\"") || !unwrapped.endsWith("\"")) {
                return unwrapped;
            }
            unwrapped = objectMapper.readValue(unwrapped, String.class).trim();
        }
        return unwrapped;
    }
}
