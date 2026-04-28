package com.paymentsserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Value("${cors.server-origin}")
    private String serverOrigin;

    @Value("${cors.server-origin-swagger}")
    private String serverOriginSwagger;

    @Value("${cors.pay-server-origin}")
    private String payServerOrigin;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);

        // 허용할 도메인 지정
        config.addAllowedOrigin("http://localhost:8080"); // 로컬 auth-server
        config.addAllowedOrigin("http://localhost:8082"); // 로컬 pay-server
        config.addAllowedOrigin("http://localhost:3001"); // 로컬 프론트엔드
        config.addAllowedOrigin(payServerOrigin); // 배포 pay-server (HTML 페이지)
        config.addAllowedOrigin(serverOrigin); // https://baeminjun.store
        config.addAllowedOrigin(serverOriginSwagger);

        config.addAllowedHeader("*"); // 모든 헤더 허용
        config.addAllowedMethod("*"); // 모든 HTTP 메서드 허용 (GET, POST, PUT, DELETE 등)

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
