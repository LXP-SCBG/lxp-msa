package com.ohgiraffers.memberservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화. BaseEntity의 @CreatedDate/@LastModifiedDate가
 * 이 설정이 있어야 실제로 값이 채워진다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
