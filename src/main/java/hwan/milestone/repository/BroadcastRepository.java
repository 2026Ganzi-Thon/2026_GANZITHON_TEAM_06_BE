package hwan.milestone.repository;

import hwan.milestone.domain.QueuedBroadcast;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 방송 대기열과 쿨다운 상태 (메모리).
 */
@Repository
public class BroadcastRepository {

    /** roomId -> 대기 중인 방송 */
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<QueuedBroadcast>> queues = new ConcurrentHashMap<>();
    /** sessionId -> 마지막 방송 요청 시각 */
    private final ConcurrentHashMap<String, Long> lastSubmittedAt = new ConcurrentHashMap<>();
    /** roomId -> 마지막 송출 시각 */
    private final ConcurrentHashMap<String, Long> lastDispatchedAt = new ConcurrentHashMap<>();

    /**
     * 쿨다운을 원자적으로 점유한다.
     *
     * @return 0 이면 점유 성공, 양수면 남은 쿨다운(ms)
     */
    public long tryAcquireCooldown(String sessionId, long now, long cooldownMs) {
        long[] remaining = new long[1];
        lastSubmittedAt.compute(sessionId, (sid, last) -> {
            if (last == null || now - last >= cooldownMs) {
                return now;
            }
            remaining[0] = cooldownMs - (now - last);
            return last;
        });
        return remaining[0];
    }

    /** 점유했던 쿨다운을 되돌린다 (대기열이 가득 찬 경우). */
    public void releaseCooldown(String sessionId, long acquiredAt) {
        lastSubmittedAt.remove(sessionId, acquiredAt);
    }

    public void deleteCooldown(String sessionId) {
        lastSubmittedAt.remove(sessionId);
    }

    public int queueSize(String roomId) {
        ConcurrentLinkedQueue<QueuedBroadcast> q = queues.get(roomId);
        return q == null ? 0 : q.size();
    }

    public void enqueue(QueuedBroadcast broadcast) {
        queues.computeIfAbsent(broadcast.roomId(), k -> new ConcurrentLinkedQueue<>()).add(broadcast);
    }

    public Set<String> roomsWithQueue() {
        return queues.keySet();
    }

    /** 마지막 송출 후 intervalMs 가 지났으면 하나를 꺼낸다. */
    public Optional<QueuedBroadcast> pollIfDue(String roomId, long now, long intervalMs) {
        ConcurrentLinkedQueue<QueuedBroadcast> q = queues.get(roomId);
        if (q == null || q.isEmpty()) {
            return Optional.empty();
        }
        Long last = lastDispatchedAt.get(roomId);
        if (last != null && now - last < intervalMs) {
            return Optional.empty();
        }
        QueuedBroadcast next = q.poll();
        if (next != null) {
            lastDispatchedAt.put(roomId, now);
        }
        return Optional.ofNullable(next);
    }
}
