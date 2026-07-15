package com.ohgiraffers.enrollmentservice.config;

import com.ohgiraffers.enrollmentservice.common.auth.LoginMemberArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 공통 설정.
 *
 * <p>CORS는 단일 진입점인 게이트웨이에서 처리한다(횡단 관심사).
 * 여기서는 {@code @LoginMember} 리졸버 등록만 한다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver());
    }
}
