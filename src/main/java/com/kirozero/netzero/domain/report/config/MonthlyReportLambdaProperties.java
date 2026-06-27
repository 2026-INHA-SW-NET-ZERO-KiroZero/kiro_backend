package com.kirozero.netzero.domain.report.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "netzero3.report.lambda")
public class MonthlyReportLambdaProperties {

    private boolean enabled = false;
    private String functionName = "";
    private String region = "us-east-1";
    private String cron = "0 0 9 1 * *";
    private String zone = "Asia/Seoul";
}
