package com.ganzithon.station.realtime.message;

import com.ganzithon.station.realtime.store.BroadcastStore.OnAirSnapshot;
import com.ganzithon.station.realtime.store.QueuedBroadcast;
import com.ganzithon.station.room.entity.MoodChip;

/**
 * ROOM_STATE 안의 "지금 전광판에 나와 있는 방송" (BR-02). 방송 도중에 입장해도 남은 시간 동안 같은 방송을 보여 줍니다.
 * 필드는 BROADCAST와 같고, 중간부터 보는 거라 displayMs 대신 remainingMs를 주고 지금까지의 리액션 수를 더했습니다.
 *
 * @param ticketNumber 보낸 승객
 * @param sentence     전광판 문장
 * @param remainingMs  전광판에 남은 노출 시간 (ms). 지나면 기본 안내 문구로 돌아갑니다
 * @param nodCount     지금까지 끄덕끄덕한 승객 수
 * @param sendCount    지금까지 전해요를 누른 승객 수
 */
public record OnAirBroadcastMessage(
        String broadcastId,
        int ticketNumber,
        String displayName,
        MoodChip moodChip,
        String situationChipCode,
        String situationChipLabel,
        String sentence,
        long remainingMs,
        int nodCount,
        int sendCount
) {
    public static OnAirBroadcastMessage from(OnAirSnapshot snapshot) {
        QueuedBroadcast broadcast = snapshot.broadcast();
        return new OnAirBroadcastMessage(
                broadcast.broadcastId(),
                broadcast.ticketNumber(),
                broadcast.displayName(),
                broadcast.moodChip(),
                broadcast.situationChipCode(),
                broadcast.situationChipLabel(),
                BroadcastMessage.sentence(broadcast),
                snapshot.remainingMs(),
                snapshot.counts().nodCount(),
                snapshot.counts().sendCount());
    }
}
