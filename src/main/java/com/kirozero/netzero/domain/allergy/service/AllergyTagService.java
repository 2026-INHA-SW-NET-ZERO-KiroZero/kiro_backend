package com.kirozero.netzero.domain.allergy.service;

import com.kirozero.netzero.domain.allergy.dto.AllergyTagItemResponse;
import com.kirozero.netzero.domain.allergy.dto.AllergyTagListResponse;
import com.kirozero.netzero.domain.allergy.enums.AllergyTag;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AllergyTagService {

    public AllergyTagListResponse getAllergyTags() {
        return new AllergyTagListResponse(
                Arrays.stream(AllergyTag.values())
                        .map(AllergyTagItemResponse::from)
                        .toList()
        );
    }

    public List<String> normalizeAndValidate(List<String> allergyTags) {
        if (allergyTags == null) {
            return List.of();
        }

        return allergyTags.stream()
                .filter(StringUtils::hasText)
                .map(this::normalize)
                .peek(this::validate)
                .distinct()
                .toList();
    }

    private String normalize(String tag) {
        return tag.trim().toLowerCase(Locale.ROOT);
    }

    private void validate(String tag) {
        if (AllergyTag.findByTag(tag).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown allergy tag: " + tag);
        }
    }
}
