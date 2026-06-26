package com.kirozero.netzero.domain.allergy.dto;

import com.kirozero.netzero.domain.allergy.enums.AllergyTag;

public record AllergyTagItemResponse(
        String tag,
        String labelKo,
        String description
) {

    public static AllergyTagItemResponse from(AllergyTag allergyTag) {
        return new AllergyTagItemResponse(
                allergyTag.tag(),
                allergyTag.labelKo(),
                allergyTag.description()
        );
    }
}
