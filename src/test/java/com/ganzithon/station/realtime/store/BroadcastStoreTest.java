package com.ganzithon.station.realtime.store;

import com.ganzithon.station.global.config.RealtimeProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 방송 쿨다운·대기열·송출 간격 (Spring 없이, 시각을 직접 넘김).
 * 쿨다운 30초, 노출 15초, 대기열 상한 2.
 */
class BroadcastStoreTest {

    private static final long COOLDOWN = 30_000;
    private static final long DISPLAY = 15_000;

    private final BroadcastStore store = new BroadcastStore(
            new RealtimeProperties(20, Duration.ofMillis(COOLDOWN), Duration.ofMillis(DISPLAY), 2));

    @Test
    void 쿨다운_안에는_남은_시간을_돌려주고_지나면_다시_점유한다() {
        assertThat(store.tryAcquireCooldown(1L, 0)).isZero();

        assertThat(store.tryAcquireCooldown(1L, 10_000)).isEqualTo(20_000);
        assertThat(store.tryAcquireCooldown(1L, COOLDOWN)).isZero();
    }

    @Test
    void 쿨다운은_승차권마다_따로다() {
        store.tryAcquireCooldown(1L, 0);

        assertThat(store.tryAcquireCooldown(2L, 0)).isZero();
    }

    @Test
    void 되돌린_쿨다운은_바로_다시_점유할_수_있다() {
        store.tryAcquireCooldown(1L, 0);

        store.releaseCooldown(1L, 0);

        assertThat(store.tryAcquireCooldown(1L, 1)).isZero();
    }

    @Test
    void 대기열이_상한에_닿으면_가득_찼다고_알린다() {
        assertThat(store.enqueue(broadcast("a", "RESULT"))).isEqualTo(1);
        assertThat(store.isQueueFull("RESULT")).isFalse();
        assertThat(store.enqueue(broadcast("b", "RESULT"))).isEqualTo(2);

        assertThat(store.isQueueFull("RESULT")).isTrue();
        assertThat(store.isQueueFull("NO_REPLY")).isFalse();
    }

    @Test
    void 첫_방송은_바로_나가고_다음은_노출_시간_뒤에_나간다() {
        store.enqueue(broadcast("a", "RESULT"));
        store.enqueue(broadcast("b", "RESULT"));

        assertThat(store.pollDue(0)).extracting(QueuedBroadcast::broadcastId).containsExactly("a");
        assertThat(store.pollDue(DISPLAY - 1)).isEmpty();
        assertThat(store.pollDue(DISPLAY)).extracting(QueuedBroadcast::broadcastId).containsExactly("b");
        assertThat(store.pollDue(DISPLAY * 2)).isEmpty();
    }

    @Test
    void 송출_간격은_방마다_따로다() {
        store.enqueue(broadcast("a", "RESULT"));
        store.enqueue(broadcast("b", "NO_REPLY"));

        assertThat(store.pollDue(0)).extracting(QueuedBroadcast::broadcastId).containsExactlyInAnyOrder("a", "b");
    }

    private static QueuedBroadcast broadcast(String broadcastId, String roomCode) {
        return new QueuedBroadcast(broadcastId, roomCode, 1, "1번 양", null, "CHIP", "문구", 0);
    }
}
