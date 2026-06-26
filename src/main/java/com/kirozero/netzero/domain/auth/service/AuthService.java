package com.kirozero.netzero.domain.auth.service;

import com.kirozero.netzero.domain.auth.dto.AuthResponse;
import com.kirozero.netzero.domain.auth.dto.CurrentUserResponse;
import com.kirozero.netzero.domain.auth.dto.LoginRequest;
import com.kirozero.netzero.domain.auth.dto.SignupRequest;
import com.kirozero.netzero.domain.user.entity.User;
import com.kirozero.netzero.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final PasswordHashService passwordHashService;
    private final AuthTokenService authTokenService;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        validateInhaEmail(email);

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists.");
        }

        User user = User.create(
                email,
                passwordHashService.hash(request.password()),
                request.nickname().trim(),
                request.cookingSkill()
        );
        user.replaceAllergies(normalizeTags(request.allergyTags()));

        User savedUser = userRepository.save(user);
        String token = authTokenService.issue(savedUser);
        return AuthResponse.of(savedUser, token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findWithAllergiesByEmail(email)
                .orElseThrow(this::badCredentials);

        if (!passwordHashService.matches(request.password(), user.getPasswordHash())) {
            throw badCredentials();
        }

        String token = authTokenService.issue(user);
        return AuthResponse.of(user, token);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String authorizationHeader) {
        return CurrentUserResponse.from(requireUser(authorizationHeader));
    }

    @Transactional(readOnly = true)
    public User requireUser(String authorizationHeader) {
        Long userId = authTokenService.parseUserId(extractBearerToken(authorizationHeader));
        return userRepository.findWithAllergiesById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateInhaEmail(String email) {
        if (!email.endsWith("@inha.edu") && !email.endsWith("@inha.ac.kr")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Inha University email is allowed.");
        }
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

    private String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is required.");
        }

        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }

    private ResponseStatusException badCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }
}
