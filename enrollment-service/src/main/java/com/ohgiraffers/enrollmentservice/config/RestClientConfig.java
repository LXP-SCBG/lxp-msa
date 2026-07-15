package com.ohgiraffers.enrollmentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    public static final String MEMBER_API_URI = "http://localhost:9080";
    public static final String LECTURE_API_URI = "http://localhost:9081";

    @Bean
    public RestClient memberRestClient() {
        return RestClient.builder()
                .baseUrl(MEMBER_API_URI)
                .build();
    }

    @Bean
    public RestClient lectureRestClient() {
        return RestClient.builder()
                .baseUrl(LECTURE_API_URI)
                .build();
    }
}
