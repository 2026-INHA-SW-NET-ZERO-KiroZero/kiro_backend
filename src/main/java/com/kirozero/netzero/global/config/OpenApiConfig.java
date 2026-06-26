package com.kirozero.netzero.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("KiroZero Backend API")
                .description("냉장고 반상회 MVP 백엔드 API 문서")
                .version("v1.0.0");

        SecurityScheme bearerScheme = new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("opaque-token")
                .description("POST /api/v1/auth/login 응답의 token 값만 입력합니다.");

        return new OpenAPI()
                .info(info)
                .servers(List.of(new Server().url("/").description("Default")))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme));
    }
}
