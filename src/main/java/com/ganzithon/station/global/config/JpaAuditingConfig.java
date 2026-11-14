package com.ganzithon.station.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseTimeEntity의 created_at, updated_at 자동 기록을 켭니다.
 * 메인 클래스가 아니라 여기 두어서 @WebMvcTest 같은 일부 테스트에서 JPA 없이도 뜨게 합니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
