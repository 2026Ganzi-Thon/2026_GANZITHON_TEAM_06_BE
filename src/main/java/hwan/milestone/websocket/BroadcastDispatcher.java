package hwan.milestone.websocket;

import hwan.milestone.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 방별 방송 대기열 송출. 500ms 마다 검사해서, 방의 마지막 송출로부터 15초가 지났으면 하나를 BROADCAST 로 보낸다.
 * (대기열이 비어 있던 방의 첫 방송은 바로 나가고, 밀려 있으면 15초 간격으로 나간다.)
 */
@Component
@RequiredArgsConstructor
public class BroadcastDispatcher {

    private final BroadcastService broadcastService;
    private final RoomMessenger messenger;

    @Scheduled(fixedRate = 500)
    public void dispatch() {
        broadcastService.collectDue().forEach(messenger::toRoom);
    }
}
