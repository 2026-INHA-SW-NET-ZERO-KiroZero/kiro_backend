package com.kirozero.netzero.domain.ai.config;

import com.kirozero.netzero.domain.ai.enums.AiProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "netzero3.llm")
public class AiGenerationProperties {

    private AiProvider provider = AiProvider.STUB;
    private boolean fallbackEnabled = true;
    private Claude claude = new Claude();

    @Getter
    @Setter
    public static class Claude {

        private String apiKey = "";
        private String baseUrl = "https://api.anthropic.com";
        private String version = "2023-06-01";
        private String model = "claude-sonnet-4-5";
        private int maxTokens = 6000;
        private int timeoutSeconds = 30;
    }
}
