package com.kirozero.netzero.domain.result.dto;

import com.kirozero.netzero.domain.slot.enums.SlotStatus;
import java.math.BigDecimal;

public record CreateConsumptionRecordResponse(
        Long recordId,
        Long slotId,
        int refundScore,
        int refundAmountPerUser,
        BigDecimal totalUsedGrams,
        BigDecimal estimatedCarbonSavedKgco2e,
        boolean campusReportLogged,
        SlotStatus nextStatus
) {
}
