package com.kirozero.netzero.domain.cooking.dto;

import java.util.List;

public record ParticipantTaskResponse(
        Long participantId,
        String nickname,
        String taskName,
        String taskPurpose,
        String taskDetail,
        List<String> attentionPoints,
        String displayInstruction,
        String skillRequired
) {
}
