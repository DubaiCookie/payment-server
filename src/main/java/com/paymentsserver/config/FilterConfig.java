package com.paymentsserver.config;

import com.paymentsserver.filter.JwtAuthenticationFilter;
import com.paymentsserver.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 필터 설정
 * JWT 인증 필터를 등록하고 보호된 엔드포인트를 설정합니다.
 */
@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter() {
        FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();

        // 필터 인스턴스 생성
        registrationBean.setFilter(new JwtAuthenticationFilter(jwtUtil));

        // 필터 적용 URL 패턴 (인증 필요)
        // 결제 관련 모든 API는 인증 필요
        registrationBean.addUrlPatterns("/payments/*");
        registrationBean.addUrlPatterns("/refunds/*");

        // 필터 순서 설정 (낮을수록 먼저 실행)
        registrationBean.setOrder(2);

        return registrationBean;
    }
}
