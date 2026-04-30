package com.paymentsserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Value("${app.ingress-url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI payServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pay Server API")
                        .description("Toss Payment 기반 결제/환불 서비스 REST API 문서")
                        .version("v1.0.0"))
                .servers(List.of(
                        new Server().url(serverUrl).description("Swagger Ingress")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 후 발급된 Access Token을 입력하세요.")));
    }
}
