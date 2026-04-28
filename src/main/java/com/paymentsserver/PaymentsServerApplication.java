package com.paymentsserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: @Scheduled 어노테이션이 붙은 스케줄러 빈을 활성화한다
@SpringBootApplication
@EnableScheduling
public class PaymentsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentsServerApplication.class, args);
    }
}
