package com.kirozero.netzero.domain.allergy.dto;

import java.util.List;

public record AllergyTagListResponse(
        List<AllergyTagItemResponse> allergyTags
) {
}
