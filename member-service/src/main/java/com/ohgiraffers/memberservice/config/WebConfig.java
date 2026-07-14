package com.ohgiraffers.memberservice.config;

import com.ohgiraffers.memberservice.common.auth.LoginMemberArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 공통 설정.
 *
 * <ul>
 *   <li>CORS: Next.js(localhost:3000)와 출처가 다르므로, 세션 쿠키(JSESSIONID)를
 *       주고받을 수 있도록 allowCredentials(true) + 정확한 출처 명시가 필요하다.
 *       allowCredentials(true) 일 때는 allowedOrigins 에 "*" 를 쓸 수 없다.</li>
 *   <li>{@link LoginMemberArgumentResolver} 등록 → 전 도메인에서 {@code @LoginMember} 사용 가능.</li>
 * </ul>
 *
 * <p>프론트엔드는 요청 시 {@code credentials: 'include'} 를 함께 보내야 쿠키가 전송된다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver());
    }
}
