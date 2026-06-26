package com.kirozero.netzero.domain.session.dto;

import java.util.List;

public record MySessionListResponse(
        List<MySessionItemResponse> sessions
) {
}
