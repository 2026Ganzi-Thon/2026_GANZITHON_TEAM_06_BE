package com.ganzithon.station.global.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 허용할 Origin 패턴 목록. 환경변수 CORS_ALLOWED_ORIGINS에 쉼표로 여러 개를 넣습니다.
 * 정확한 주소(https://station.example.com)와 패턴(http://localhost:*) 둘 다 됩니다.
 * WebSocket(/ws) 연결 허용에도 같은 값을 씁니다.
 */
@Validated
@ConfigurationProperties(prefix = "station.cors")
public record CorsProperties(
        @NotEmpty List<String> allowedOriginPatterns
) {
    public CorsProperties {
        allowedOriginPatterns = allowedOriginPatterns == null ? List.of() : allowedOriginPatterns.stream()
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .toList();

        // 환경변수가 없으면 Spring이 "${CORS_ALLOWED_ORIGINS}" 글자를 그대로 넣습니다.
        // 그대로 뜨면 모든 요청이 조용히 막히므로, 시작할 때 바로 실패시킵니다.
        if (allowedOriginPatterns.stream().anyMatch(pattern -> pattern.contains("${"))) {
            throw new IllegalArgumentException("환경변수 CORS_ALLOWED_ORIGINS가 설정되지 않았습니다.");
        }
    }
}
