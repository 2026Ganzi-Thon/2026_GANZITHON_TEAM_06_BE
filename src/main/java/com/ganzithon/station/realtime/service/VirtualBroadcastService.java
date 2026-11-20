package com.ganzithon.station.realtime.service;

import com.ganzithon.station.global.config.VirtualPassengerProperties;
import com.ganzithon.station.realtime.store.BroadcastStore;
import com.ganzithon.station.realtime.store.QueuedBroadcast;
import com.ganzithon.station.realtime.store.RoomPassengerStore;
import com.ganzithon.station.room.entity.SituationChip;
import com.ganzithon.station.room.entity.VirtualPassenger;
import com.ganzithon.station.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 시연용 가상 승객 자동 방송 (RM-05, 담당: 환희). 메모리만 사용합니다.
 * 호출 방향: realtime → room (가상 승객 정의와 랜덤 상황 칩).
 * <ul>
 *     <li>실제 승객이 있는 방에서만 합니다. 방에 처음 사람이 들어오고 firstDelay(10초) 뒤 첫 방송, 이후 4~6분 랜덤 간격</li>
 *     <li>실제 방송이 우선입니다. 방송 대기열이 비어 있을 때만 넣고, 차 있으면 다음 확인 때 다시 봅니다</li>
 *     <li>가상 승객은 좌석·승객 목록에 나오지 않고 방송만 합니다. 승차권 기록·쿨다운도 쓰지 않습니다</li>
 *     <li>송출은 실제 방송과 같은 대기열·간격(BroadcastDispatcher)으로 나가고, 메시지도 같은 BROADCAST입니다 (ticketNumber 1~99)</li>
 * </ul>
 * 시각(now)은 호출하는 쪽이 넘겨서, 시간이 지나는 상황을 테스트에서 바로 만들 수 있게 했습니다.
 * 스케줄러 스레드 하나에서만 불려서 따로 잠그지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class VirtualBroadcastService {

    private final RoomService roomService;
    private final RoomPassengerStore roomPassengerStore;
    private final BroadcastStore broadcastStore;
    private final VirtualPassengerProperties properties;

    /** roomCode → 다음 가상 방송 시각 (epoch ms). 실제 승객이 있는 방만 들고 있습니다 */
    private final Map<String, Long> nextBroadcastAt = new HashMap<>();

    /** 방마다 가상 방송할 때가 됐으면 대기열에 넣습니다. VirtualPassengerScheduler가 주기적으로 부릅니다. */
    public void tick(long now) {
        if (!properties.enabled()) {
            return;
        }
        Set<String> activeRooms = roomPassengerStore.findOccupiedRoomCodes();
        // 비워진 방은 잊습니다. 다시 누가 들어오면 첫 방송 대기(10초)부터 시작
        nextBroadcastAt.keySet().retainAll(activeRooms);

        for (String roomCode : activeRooms) {
            Long dueAt = nextBroadcastAt.get(roomCode);
            if (dueAt == null) {
                nextBroadcastAt.put(roomCode, now + properties.firstDelay().toMillis());
            } else if (now >= dueAt && broadcastStore.isQueueEmpty(roomCode)) {
                broadcastStore.enqueue(createBroadcast(roomCode, now));
                nextBroadcastAt.put(roomCode, now + randomIntervalMs());
            }
        }
    }

    /** 테스트용: 그 방의 다음 가상 방송 시각. 없으면 null */
    Long getNextBroadcastAt(String roomCode) {
        return nextBroadcastAt.get(roomCode);
    }

    private QueuedBroadcast createBroadcast(String roomCode, long now) {
        List<VirtualPassenger> passengers = roomService.getVirtualPassengerList(roomCode);
        VirtualPassenger passenger = passengers.get(ThreadLocalRandom.current().nextInt(passengers.size()));
        SituationChip chip = roomService.getRandomSituationChip(roomCode);
        return new QueuedBroadcast(UUID.randomUUID().toString(), roomCode, passenger.ticketNumber(),
                passenger.displayName(), passenger.moodChip(), chip.getCode(), chip.getLabel(), now);
    }

    private long randomIntervalMs() {
        long min = properties.minInterval().toMillis();
        long max = properties.maxInterval().toMillis();
        return min >= max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
    }
}
