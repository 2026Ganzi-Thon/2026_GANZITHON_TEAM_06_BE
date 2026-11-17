package com.ganzithon.station.realtime.store;

import com.ganzithon.station.global.config.RealtimeProperties;
import com.ganzithon.station.realtime.message.Reaction;
import com.ganzithon.station.realtime.store.BroadcastStore.ReactionCounts;
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

    // ── 전광판 방송 리액션 수 ──

    @Test
    void 지금_나오는_방송에만_리액션을_세고_승객_한_명당_종류마다_한_번이다() {
        store.enqueue(broadcast("a", "RESULT"));
        store.pollDue(0);

        store.countReaction("RESULT", "a", 1L, Reaction.NOD, 100);
        store.countReaction("RESULT", "a", 1L, Reaction.NOD, 200);
        store.countReaction("RESULT", "a", 1L, Reaction.AWW, 300);

        assertThat(store.countReaction("RESULT", "a", 2L, Reaction.NOD, 400))
                .contains(new ReactionCounts(2, 1));
    }

    @Test
    void 노출_시간이_지났거나_다른_방송이면_세지_않는다() {
        store.enqueue(broadcast("a", "RESULT"));
        store.pollDue(0);

        assertThat(store.countReaction("RESULT", "other", 1L, Reaction.NOD, 100)).isEmpty();
        assertThat(store.countReaction("NO_REPLY", "a", 1L, Reaction.NOD, 100)).isEmpty();
        assertThat(store.countReaction("RESULT", "a", 1L, Reaction.NOD, DISPLAY)).isEmpty();
    }

    @Test
    void 다음_방송이_나가면_새로_센다() {
        store.enqueue(broadcast("a", "RESULT"));
        store.enqueue(broadcast("b", "RESULT"));
        store.pollDue(0);
        store.countReaction("RESULT", "a", 1L, Reaction.NOD, 100);

        store.pollDue(DISPLAY);

        assertThat(store.isOnAir("RESULT", "a", DISPLAY)).isFalse();
        assertThat(store.countReaction("RESULT", "b", 1L, Reaction.AWW, DISPLAY + 100))
                .contains(new ReactionCounts(0, 1));
    }

    // ── 지금 전광판에 나와 있는 방송 (방송 도중 입장용) ──

    @Test
    void 방송을_한_번도_안_한_방은_전광판_방송이_없다() {
        assertThat(store.findOnAir("RESULT", 0)).isEmpty();
    }

    @Test
    void 나와_있는_방송은_남은_시간과_리액션_수를_주고_노출이_끝나면_없다() {
        store.enqueue(broadcast("a", "RESULT"));
        store.pollDue(1_000);
        store.countReaction("RESULT", "a", 1L, Reaction.NOD, 2_000);

        BroadcastStore.OnAirSnapshot onAir = store.findOnAir("RESULT", 5_000).orElseThrow();

        assertThat(onAir.broadcast().broadcastId()).isEqualTo("a");
        assertThat(onAir.remainingMs()).isEqualTo(DISPLAY - 4_000);
        assertThat(onAir.counts()).isEqualTo(new ReactionCounts(1, 0));
        assertThat(store.findOnAir("RESULT", 1_000 + DISPLAY)).isEmpty();
        assertThat(store.findOnAir("NO_REPLY", 5_000)).isEmpty();
    }

    private static QueuedBroadcast broadcast(String broadcastId, String roomCode) {
        return new QueuedBroadcast(broadcastId, roomCode, 1, "1번 양", null, "CHIP", "문구", 0);
    }
}
