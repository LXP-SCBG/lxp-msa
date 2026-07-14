package com.ohgiraffers.memberservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 해시 인코더.
 *
 * <p>BCrypt 사용을 위해 spring-security-crypto 의존성만 추가하면 되며,
 * spring-boot-starter-security(전체 시큐리티 필터체인)는 사용하지 않는다.
 * 인증은 HttpSession 으로 직접 관리하기 때문이다.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
