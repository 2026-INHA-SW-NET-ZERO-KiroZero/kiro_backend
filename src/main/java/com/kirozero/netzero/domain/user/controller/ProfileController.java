package com.kirozero.netzero.domain.user.controller;

import com.kirozero.netzero.domain.auth.dto.CurrentUserResponse;
import com.kirozero.netzero.domain.user.dto.UpdateProfileRequest;
import com.kirozero.netzero.domain.user.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PutMapping
    public CurrentUserResponse updateProfile(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return profileService.updateProfile(authorization, request);
    }
}
