package com.ganzithon.station.user.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void 연속_5회_실패하면_잠금() {
        User user = User.builder().nickname("몽실이").pinHash("hashed").build();
        Instant now = Instant.parse("2026-11-17T00:00:00Z");

        for (int i = 0; i < 5; i++) {
            user.recordLoginFailure(now);
        }

        assertThat(user.isLocked(now)).isTrue();
    }

    @Test
    void 잠금_중에는_잠금_해제_전까지_계속_잠금_상태() {
        User user = User.builder().nickname("몽실이").pinHash("hashed").build();
        Instant now = Instant.parse("2026-11-17T00:00:00Z");
        for (int i = 0; i < 5; i++) {
            user.recordLoginFailure(now);
        }

        assertThat(user.isLocked(now.plusSeconds(1))).isTrue();
        assertThat(user.isLocked(now.plusSeconds(299))).isTrue();
    }

    @Test
    void 잠금_시간이_지나면_잠금_해제() {
        User user = User.builder().nickname("몽실이").pinHash("hashed").build();
        Instant now = Instant.parse("2026-11-17T00:00:00Z");
        for (int i = 0; i < 5; i++) {
            user.recordLoginFailure(now);
        }

        assertThat(user.isLocked(now.plusSeconds(300))).isFalse();
    }

    @Test
    void 잠금_해제_뒤_첫_실패는_1부터_다시_셈() {
        User user = User.builder().nickname("몽실이").pinHash("hashed").build();
        Instant now = Instant.parse("2026-11-17T00:00:00Z");
        for (int i = 0; i < 5; i++) {
            user.recordLoginFailure(now);
        }
        Instant afterUnlock = now.plusSeconds(300);

        user.recordLoginFailure(afterUnlock);

        assertThat(user.isLocked(afterUnlock)).isFalse();
        assertThat(user.getPinFailCount()).isEqualTo(1);
    }

    @Test
    void 성공하면_실패_횟수와_잠금_초기화() {
        User user = User.builder().nickname("몽실이").pinHash("hashed").build();
        Instant now = Instant.parse("2026-11-17T00:00:00Z");
        for (int i = 0; i < 5; i++) {
            user.recordLoginFailure(now);
        }

        user.resetLoginFailure();

        assertThat(user.getPinFailCount()).isEqualTo(0);
        assertThat(user.isLocked(now)).isFalse();
    }
}
