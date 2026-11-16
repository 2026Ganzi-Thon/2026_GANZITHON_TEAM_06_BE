package com.ganzithon.station.realtime.messaging;

import com.ganzithon.station.realtime.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 방송 송출 (BR-02). 500ms마다 확인해서, 방의 마지막 송출 후 노출 시간(15초)이 지났으면 대기열에서 하나를 BROADCAST로 보냅니다.
 */
@Component
@RequiredArgsConstructor
public class BroadcastDispatcher {

    private static final long CHECK_INTERVAL_MS = 500;

    private final BroadcastService broadcastService;
    private final RoomMessenger roomMessenger;

    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void dispatch() {
        broadcastService.collectDue().forEach(roomMessenger::toRoom);
    }
}
