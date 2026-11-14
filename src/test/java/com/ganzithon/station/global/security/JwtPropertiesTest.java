package com.ganzithon.station.global.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT_SECRET이 없거나 짧으면 서버가 시작하지 않아야 합니다.
 */
class JwtPropertiesTest {

    private static final String VALID_SECRET = "0123456789abcdef0123456789abcdef"; // 32바이트

    @Test
    void 비밀값이_없으면_실패() {
        assertThatThrownBy(() -> new JwtProperties(null, null))
                .hasMessageContaining("JWT_SECRET이 설정되지 않았습니다");
        assertThatThrownBy(() -> new JwtProperties(" ", null))
                .hasMessageContaining("JWT_SECRET이 설정되지 않았습니다");
    }

    @Test
    void 환경변수가_빠져서_자리표시자가_그대로_들어오면_실패() {
        assertThatThrownBy(() -> new JwtProperties("${JWT_SECRET}", null))
                .hasMessageContaining("JWT_SECRET이 설정되지 않았습니다");
    }

    @Test
    void 비밀값이_32바이트보다_짧으면_실패() {
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET.substring(1), null))
                .hasMessageContaining("32바이트 이상")
                .hasMessageContaining("현재 31바이트");
    }

    @Test
    void 비밀값이_32바이트면_통과하고_만료는_기본_7일() {
        JwtProperties properties = new JwtProperties(VALID_SECRET, null);
        assertThat(properties.expiration()).isEqualTo(Duration.ofDays(7));
    }
}
