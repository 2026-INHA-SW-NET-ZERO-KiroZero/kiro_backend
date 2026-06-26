package com.kirozero.netzero.domain.auth.dto;

import com.kirozero.netzero.domain.user.entity.User;
import com.kirozero.netzero.domain.user.entity.UserAllergy;
import com.kirozero.netzero.domain.user.enums.CookingSkill;
import java.util.List;

public record AuthResponse(
        Long userId,
        String email,
        String nickname,
        CookingSkill cookingSkill,
        int cash,
        List<String> allergyTags,
        String token
) {

    public static AuthResponse of(User user, String token) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getCookingSkill(),
                user.getCash(),
                user.getAllergies().stream()
                        .map(UserAllergy::getAllergenTag)
                        .toList(),
                token
        );
    }
}
