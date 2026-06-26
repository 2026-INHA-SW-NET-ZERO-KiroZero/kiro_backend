package com.kirozero.netzero.domain.user.service;

import com.kirozero.netzero.domain.auth.dto.CurrentUserResponse;
import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.user.dto.UpdateProfileRequest;
import com.kirozero.netzero.domain.user.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AuthService authService;

    @Transactional
    public CurrentUserResponse updateProfile(String authorizationHeader, UpdateProfileRequest request) {
        User user = authService.requireUser(authorizationHeader);
        user.updateProfile(
                request.nickname().trim(),
                request.cookingSkill(),
                normalizeTags(request.allergyTags())
        );
        return CurrentUserResponse.from(user);
    }

    private List<String> normalizeTags(List<String> allergyTags) {
        if (allergyTags == null) {
            return List.of();
        }

        return allergyTags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }
}
