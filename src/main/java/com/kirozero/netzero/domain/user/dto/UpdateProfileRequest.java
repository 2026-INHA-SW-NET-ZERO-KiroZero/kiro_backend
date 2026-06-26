package com.kirozero.netzero.domain.user.dto;

import com.kirozero.netzero.domain.user.enums.CookingSkill;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateProfileRequest(
        @NotBlank String nickname,
        @NotNull CookingSkill cookingSkill,
        List<String> allergyTags
) {
}
