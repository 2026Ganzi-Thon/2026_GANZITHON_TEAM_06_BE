package com.ganzithon.station.realtime.messaging;

import com.ganzithon.station.realtime.service.VirtualBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 가상 승객 자동 방송 (RM-05). 1초마다 방별로 방송할 때가 됐는지 확인합니다.
 * 대기열에 넣기만 하고, 실제 송출은 BroadcastDispatcher가 실제 방송과 같은 간격으로 합니다.
 */
@Component
@RequiredArgsConstructor
public class VirtualPassengerScheduler {

    private static final long CHECK_INTERVAL_MS = 1_000;

    private final VirtualBroadcastService virtualBroadcastService;

    @Scheduled(fixedDelay = CHECK_INTERVAL_MS)
    public void broadcast() {
        virtualBroadcastService.tick(System.currentTimeMillis());
    }
}
