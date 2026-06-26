package com.kirozero.netzero.domain.auth.dto;

import com.kirozero.netzero.domain.user.enums.CookingSkill;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SignupRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8)
        String password,

        @NotBlank
        String nickname,

        @NotNull
        CookingSkill cookingSkill,

        List<String> allergyTags
) {
}
