package com.kirozero.netzero.domain.auth.service;

import com.kirozero.netzero.domain.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_TTL_SECONDS = 60L * 60L * 24L * 7L;

    private final byte[] secret;

    public AuthTokenService(@Value("${netzero3.auth.token-secret}") String tokenSecret) {
        this.secret = tokenSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(User user) {
        long expiresAt = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
        String payloadPart = encodePayload(user.getId(), expiresAt);
        String signaturePart = sign(payloadPart);
        return payloadPart + "." + signaturePart;
    }

    public Long parseUserId(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2 || !sign(parts[0]).equals(parts[1])) {
            throw unauthorized();
        }

        String payload = decodePayload(parts[0]);
        String[] payloadParts = payload.split("\\|");
        if (payloadParts.length != 2) {
            throw unauthorized();
        }

        long userId = parseLong(payloadParts[0]);
        long expiresAt = parseLong(payloadParts[1]);
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw unauthorized();
        }

        return userId;
    }

    private String encodePayload(Long userId, long expiresAt) {
        String payload = userId + "|" + expiresAt;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String decodePayload(String payloadPart) {
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadPart);
            return new String(payloadBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw unauthorized();
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw unauthorized();
        }
    }

    private String sign(String payloadPart) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payloadPart.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Token signature cannot be generated.", e);
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token.");
    }
}
