package com.ohgiraffers.lectureservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // 기본값은 로컬 실행용. 도커에선 MEMBER_API_URI 환경변수로 컨테이너 주소를 주입한다.
    @Bean
    public RestClient memberRestClient(
            @Value("${member.api.uri:http://localhost:9080}") String memberApiUri) {
        return RestClient.builder()
                .baseUrl(memberApiUri)
                .build();
    }
}
