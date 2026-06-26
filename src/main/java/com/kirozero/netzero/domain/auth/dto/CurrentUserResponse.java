package com.kirozero.netzero.domain.auth.dto;

import com.kirozero.netzero.domain.user.entity.User;
import com.kirozero.netzero.domain.user.entity.UserAllergy;
import com.kirozero.netzero.domain.user.enums.CookingSkill;
import java.util.List;

public record CurrentUserResponse(
        Long userId,
        String email,
        String nickname,
        CookingSkill cookingSkill,
        List<String> allergyTags
) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getCookingSkill(),
                user.getAllergies().stream()
                        .map(UserAllergy::getAllergenTag)
                        .toList()
        );
    }
}
