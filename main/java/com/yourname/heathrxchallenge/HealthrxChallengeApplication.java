package com.kashish.heathrx_challenge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication // The magic annotation that enables auto-configuration, component scanning, etc.
public class HealthrxChallengeApplication {

    public static void main(String[] args) {
        // This line starts the entire Spring Boot application.
        SpringApplication.run(HealthrxChallengeApplication.class, args);
    }

    // This method defines our RestTemplate bean.
    // Spring will call this method once on startup to create the object.
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}