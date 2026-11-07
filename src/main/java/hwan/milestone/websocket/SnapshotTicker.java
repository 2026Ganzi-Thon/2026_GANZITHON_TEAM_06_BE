package hwan.milestone.websocket;

import hwan.milestone.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 100ms 틱: 방마다 바뀐 플레이어만 SNAPSHOT 1개로 묶어 전송. 바뀐 게 없으면 보내지 않는다. */
@Component
@RequiredArgsConstructor
public class SnapshotTicker {

    private final RoomService roomService;
    private final RoomMessenger messenger;

    @Scheduled(fixedRate = 100)
    public void tick() {
        roomService.collectDirtySnapshots().forEach(messenger::toRoom);
    }
}
