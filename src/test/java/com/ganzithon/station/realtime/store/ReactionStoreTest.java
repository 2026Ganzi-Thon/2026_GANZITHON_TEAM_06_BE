package com.ganzithon.station.realtime.store;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 리액션 연타 제한 (Spring 없이, 시각을 직접 넘김). 승차권마다 1초에 1번.
 */
class ReactionStoreTest {

    private final ReactionStore store = new ReactionStore();

    @Test
    void 일초_안에는_다시_보낼_수_없고_지나면_보낼_수_있다() {
        assertThat(store.tryAcquire(1L, 0)).isTrue();

        assertThat(store.tryAcquire(1L, 999)).isFalse();
        assertThat(store.tryAcquire(1L, ReactionStore.INTERVAL_MS)).isTrue();
    }

    @Test
    void 제한은_승차권마다_따로다() {
        store.tryAcquire(1L, 0);

        assertThat(store.tryAcquire(2L, 0)).isTrue();
    }
}
