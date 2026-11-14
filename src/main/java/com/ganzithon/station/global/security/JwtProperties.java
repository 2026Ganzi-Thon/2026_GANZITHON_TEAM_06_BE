package com.ganzithon.station.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 계정 토큰(JWT) 설정. 환경변수 JWT_SECRET이 없거나 32바이트보다 짧으면 서버가 시작하지 않습니다.
 *
 * @param secret     HS256 서명 키 (UTF-8 기준 32바이트 이상)
 * @param expiration 토큰 유효 기간 (기본 7일, 리프레시 토큰 없음)
 */
@ConfigurationProperties(prefix = "station.jwt")
public record JwtProperties(
        String secret,
        Duration expiration
) {
    private static final int MIN_SECRET_BYTES = 32;
    private static final Duration DEFAULT_EXPIRATION = Duration.ofDays(7);

    public JwtProperties {
        // 환경변수가 없으면 Spring이 "${JWT_SECRET}" 글자를 그대로 넣으므로 그것도 '없음'으로 봅니다
        if (secret == null || secret.isBlank() || secret.contains("${")) {
            throw new IllegalArgumentException("환경변수 JWT_SECRET이 설정되지 않았습니다.");
        }
        int secretBytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (secretBytes < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "JWT_SECRET은 " + MIN_SECRET_BYTES + "바이트 이상이어야 합니다. (현재 " + secretBytes + "바이트)");
        }
        if (expiration == null) {
            expiration = DEFAULT_EXPIRATION;
        }
    }

    public byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
