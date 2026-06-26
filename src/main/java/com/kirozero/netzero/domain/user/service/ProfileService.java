package com.kirozero.netzero.domain.user.service;

import com.kirozero.netzero.domain.auth.dto.CurrentUserResponse;
import com.kirozero.netzero.domain.auth.service.AuthService;
import com.kirozero.netzero.domain.allergy.service.AllergyTagService;
import com.kirozero.netzero.domain.user.dto.UpdateProfileRequest;
import com.kirozero.netzero.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AuthService authService;
    private final AllergyTagService allergyTagService;

    @Transactional
    public CurrentUserResponse updateProfile(String authorizationHeader, UpdateProfileRequest request) {
        User user = authService.requireUser(authorizationHeader);
        user.updateProfile(
                request.nickname().trim(),
                request.cookingSkill(),
                allergyTagService.normalizeAndValidate(request.allergyTags())
        );
        return CurrentUserResponse.from(user);
    }
}
