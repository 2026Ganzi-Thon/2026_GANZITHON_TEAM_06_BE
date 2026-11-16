package com.ganzithon.station.realtime.store;

import com.ganzithon.station.room.entity.MoodChip;

/**
 * 대기열에서 송출을 기다리는 방송 (메모리). 방송은 다시 조회하지 않아서 DB에 남기지 않습니다.
 * 보낸 승객이 나가도 그대로 송출되도록 필요한 값을 모두 복사해 둡니다.
 */
public record QueuedBroadcast(
        String broadcastId,
        String roomCode,
        int ticketNumber,
        String displayName,
        MoodChip moodChip,
        String situationChipCode,
        String situationChipLabel,
        long queuedTs
) {
}
