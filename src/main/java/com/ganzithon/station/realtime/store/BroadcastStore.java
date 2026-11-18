package com.ganzithon.station.realtime.store;

import com.ganzithon.station.global.config.RealtimeProperties;
import com.ganzithon.station.realtime.message.Reaction;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 방송 대기열(방별)과 쿨다운(승차권별). 메모리만 씁니다.
 * 시각(now)은 호출하는 쪽이 넘겨서, 시간이 지나는 상황을 테스트에서 바로 만들 수 있게 했습니다.
 */
@Component
public class BroadcastStore {

    /** roomCode → 송출을 기다리는 방송 (먼저 온 순서) */
    private final Map<String, Deque<QueuedBroadcast>> queues = new HashMap<>();
    /** roomCode → 마지막 송출 시각 */
    private final Map<String, Long> lastDispatchedAt = new HashMap<>();
    /** roomCode → 마지막으로 전광판에 나간 방송과 그 리액션 수. 다음 방송이 나가면 바뀝니다 */
    private final Map<String, OnAir> onAir = new HashMap<>();
    /** ticketId → 마지막 방송 시각. 재접속해도 쿨다운을 피할 수 없게 세션이 아니라 승차권 기준 */
    private final Map<Long, Long> lastBroadcastAt = new HashMap<>();

    private final long cooldownMs;
    private final long displayMs;
    private final int queueSize;

    public BroadcastStore(RealtimeProperties realtimeProperties) {
        this.cooldownMs = realtimeProperties.broadcastCooldown().toMillis();
        this.displayMs = realtimeProperties.broadcastDisplay().toMillis();
        this.queueSize = realtimeProperties.broadcastQueueSize();
    }

    /**
     * 쿨다운을 점유합니다. 확인과 기록을 한 번에 해서, 두 번 빠르게 보내도 하나만 통과합니다.
     *
     * @return 0이면 점유 성공, 양수면 남은 쿨다운(ms)
     */
    public synchronized long tryAcquireCooldown(Long ticketId, long now) {
        Long last = lastBroadcastAt.get(ticketId);
        if (last != null && now - last < cooldownMs) {
            return cooldownMs - (now - last);
        }
        lastBroadcastAt.put(ticketId, now);
        return 0;
    }

    /** 점유한 쿨다운을 되돌립니다 (방송 기록이 실패한 경우). 그 사이 다시 점유됐으면 건드리지 않습니다. */
    public synchronized void releaseCooldown(Long ticketId, long acquiredAt) {
        lastBroadcastAt.remove(ticketId, acquiredAt);
    }

    public synchronized boolean isQueueFull(String roomCode) {
        Deque<QueuedBroadcast> queue = queues.get(roomCode);
        return queue != null && queue.size() >= queueSize;
    }

    /** 송출을 기다리는 방송이 없는지 (가상 승객은 이때만 방송해서 실제 방송이 우선) */
    public synchronized boolean isQueueEmpty(String roomCode) {
        Deque<QueuedBroadcast> queue = queues.get(roomCode);
        return queue == null || queue.isEmpty();
    }

    /** @return 대기 순번 (1이면 다음 차례) */
    public synchronized int enqueue(QueuedBroadcast broadcast) {
        Deque<QueuedBroadcast> queue = queues.computeIfAbsent(broadcast.roomCode(), roomCode -> new ArrayDeque<>());
        queue.addLast(broadcast);
        return queue.size();
    }

    /**
     * 송출할 방송. 방마다 마지막 송출 후 노출 시간이 지났으면 하나씩 꺼냅니다.
     * 대기열이 비어 있던 방의 첫 방송은 바로 나가고, 밀려 있으면 노출 시간 간격으로 나갑니다.
     * 끝난 쿨다운 기록도 여기서 정리합니다.
     */
    public synchronized List<QueuedBroadcast> pollDue(long now) {
        List<QueuedBroadcast> due = new ArrayList<>();
        Iterator<Map.Entry<String, Deque<QueuedBroadcast>>> iterator = queues.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Deque<QueuedBroadcast>> entry = iterator.next();
            Long last = lastDispatchedAt.get(entry.getKey());
            if (last != null && now - last < displayMs) {
                continue;
            }
            QueuedBroadcast broadcast = entry.getValue().pollFirst();
            due.add(broadcast);
            lastDispatchedAt.put(entry.getKey(), now);
            onAir.put(entry.getKey(), new OnAir(broadcast, now));
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
        lastBroadcastAt.values().removeIf(broadcastAt -> now - broadcastAt >= cooldownMs);
        return due;
    }

    /** 이 방송이 지금 그 방 전광판에 나오고 있는지 (송출 후 노출 시간 안) */
    public synchronized boolean isOnAir(String roomCode, String broadcastId, long now) {
        OnAir current = onAirNow(roomCode, now);
        return current != null && current.broadcast.broadcastId().equals(broadcastId);
    }

    /**
     * 지금 그 방 전광판에 나와 있는 방송 (방송 도중에 입장한 승객에게 보여 주기용).
     * 방송을 한 번도 안 했거나, 마지막 방송의 노출 시간이 끝났으면 empty
     */
    public synchronized Optional<OnAirSnapshot> findOnAir(String roomCode, long now) {
        OnAir current = onAirNow(roomCode, now);
        if (current == null) {
            return Optional.empty();
        }
        return Optional.of(new OnAirSnapshot(current.broadcast, displayMs - (now - current.dispatchedAt),
                current.counts()));
    }

    /**
     * 전광판 방송에 리액션을 셉니다. 한 방송에 승객 한 명당 리액션 종류마다 한 번만 셉니다 (연타로 숫자가 부풀지 않게).
     *
     * @return 센 뒤의 그 방송 리액션 수. 지금 나오는 방송이 아니면 empty
     */
    public synchronized Optional<ReactionCounts> countReaction(String roomCode, String broadcastId, Long ticketId,
                                                               Reaction reaction, long now) {
        if (!isOnAir(roomCode, broadcastId, now)) {
            return Optional.empty();
        }
        OnAir current = onAir.get(roomCode);
        (reaction == Reaction.NOD ? current.nodded : current.awwed).add(ticketId);
        return Optional.of(current.counts());
    }

    private OnAir onAirNow(String roomCode, long now) {
        OnAir current = onAir.get(roomCode);
        return current != null && now - current.dispatchedAt < displayMs ? current : null;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public long getDisplayMs() {
        return displayMs;
    }

    /**
     * 전광판 방송 하나의 리액션 수.
     *
     * @param nodCount 끄덕임한 승객 수
     * @param awwCount 짠함한 승객 수
     */
    public record ReactionCounts(int nodCount, int awwCount) {
    }

    /**
     * 지금 전광판에 나와 있는 방송.
     *
     * @param broadcast   송출한 방송
     * @param remainingMs 전광판에 남은 노출 시간
     * @param counts      지금까지의 리액션 수
     */
    public record OnAirSnapshot(QueuedBroadcast broadcast, long remainingMs, ReactionCounts counts) {
    }

    private static final class OnAir {
        private final QueuedBroadcast broadcast;
        private final long dispatchedAt;
        private final Set<Long> nodded = new HashSet<>();
        private final Set<Long> awwed = new HashSet<>();

        private OnAir(QueuedBroadcast broadcast, long dispatchedAt) {
            this.broadcast = broadcast;
            this.dispatchedAt = dispatchedAt;
        }

        private ReactionCounts counts() {
            return new ReactionCounts(nodded.size(), awwed.size());
        }
    }
}
