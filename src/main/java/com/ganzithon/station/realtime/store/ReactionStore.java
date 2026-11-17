package com.ganzithon.station.realtime.store;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 리액션 연타 제한 (승차권마다 1초에 1번). 메모리만 씁니다.
 * 시각(now)은 호출하는 쪽이 넘겨서, 시간이 지나는 상황을 테스트에서 바로 만들 수 있게 했습니다.
 */
@Component
public class ReactionStore {

    /** 같은 승객의 리액션 사이 최소 간격 ("1초 리액션") */
    public static final long INTERVAL_MS = 1_000;

    /** ticketId → 마지막 리액션 시각. 재접속해도 같은 승차권이면 같은 제한 */
    private final Map<Long, Long> lastReactionAt = new HashMap<>();

    /**
     * 리액션을 보낼 수 있으면 기록하고 true. 확인과 기록을 한 번에 해서, 동시에 여러 번 보내도 하나만 통과합니다.
     * 지난 기록도 여기서 정리합니다 (승객 수만큼이라 작음).
     */
    public synchronized boolean tryAcquire(Long ticketId, long now) {
        lastReactionAt.values().removeIf(reactedAt -> now - reactedAt >= INTERVAL_MS);
        if (lastReactionAt.containsKey(ticketId)) {
            return false;
        }
        lastReactionAt.put(ticketId, now);
        return true;
    }
}
