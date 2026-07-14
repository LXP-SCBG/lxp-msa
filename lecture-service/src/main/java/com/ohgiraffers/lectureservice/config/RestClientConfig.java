package com.ohgiraffers.lectureservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final String MEMBER_API_URI = "http://localhost:8080";

    @Bean
    public RestClient memberRestClient() {
        return RestClient.builder()
                .baseUrl(MEMBER_API_URI)
                .build();
    }
}
