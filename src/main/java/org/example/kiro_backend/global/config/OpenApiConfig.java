package org.example.kiro_backend.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Kiro Backend API")
                .description("Kiro 백엔드 API 문서")
                .version("v1.0.0");

        return new OpenAPI()
                .info(info)
                .servers(List.of(
                        new Server().url("/").description("Default")
                ));
    }
}
