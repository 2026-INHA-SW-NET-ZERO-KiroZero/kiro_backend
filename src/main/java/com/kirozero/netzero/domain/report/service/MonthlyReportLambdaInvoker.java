package com.kirozero.netzero.domain.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kirozero.netzero.domain.report.config.MonthlyReportLambdaProperties;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

@Service
@RequiredArgsConstructor
public class MonthlyReportLambdaInvoker {

    private static final Logger log = LoggerFactory.getLogger(MonthlyReportLambdaInvoker.class);

    private final MonthlyReportLambdaProperties properties;
    private final ObjectMapper objectMapper;

    public boolean invoke(YearMonth reportMonth, LocalDateTime triggeredAt) {
        if (!properties.isEnabled()) {
            log.info("monthly report lambda is disabled");
            return false;
        }
        if (!StringUtils.hasText(properties.getFunctionName())) {
            log.warn("monthly report lambda function name is empty");
            return false;
        }

        String payload = writePayload(reportMonth, triggeredAt);
        InvokeRequest request = InvokeRequest.builder()
                .functionName(properties.getFunctionName())
                .invocationType(InvocationType.EVENT)
                .payload(SdkBytes.fromUtf8String(payload))
                .build();

        try (LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.of(properties.getRegion()))
                .build()) {
            lambdaClient.invoke(request);
        }

        log.info("monthly report lambda invoked: functionName={}, reportMonth={}",
                properties.getFunctionName(), reportMonth);
        return true;
    }

    private String writePayload(YearMonth reportMonth, LocalDateTime triggeredAt) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "reportMonth", reportMonth.toString(),
                    "triggeredAt", triggeredAt.toString(),
                    "source", "kiro-backend-monthly-report-scheduler"
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize monthly report lambda payload", e);
        }
    }
}
