package com.paymentsserver.config;

import com.paymentsserver.filter.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter() {
        FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new JwtAuthenticationFilter());
        registrationBean.addUrlPatterns("/payments");
        registrationBean.addUrlPatterns("/payments/*");
        registrationBean.addUrlPatterns("/refunds/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }
}
