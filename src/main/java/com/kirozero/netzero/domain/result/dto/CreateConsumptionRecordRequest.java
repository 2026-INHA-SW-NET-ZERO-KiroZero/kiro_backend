package com.kirozero.netzero.domain.result.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateConsumptionRecordRequest(
        @NotNull Integer finishedFoodRate,
        @NotBlank String cookedPhotoUrl,
        @NotBlank String afterPhotoUrl,
        @NotEmpty List<@Valid ConsumptionRecordItemRequest> items
) {
}
