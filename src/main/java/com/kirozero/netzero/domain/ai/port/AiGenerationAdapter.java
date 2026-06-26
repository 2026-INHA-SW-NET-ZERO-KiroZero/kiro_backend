package com.kirozero.netzero.domain.ai.port;

import com.kirozero.netzero.domain.ai.enums.AiProvider;

public interface AiGenerationAdapter extends AiGenerationPort {

    AiProvider provider();
}
