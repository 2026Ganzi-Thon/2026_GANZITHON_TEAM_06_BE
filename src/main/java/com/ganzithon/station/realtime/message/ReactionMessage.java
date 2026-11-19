package com.ganzithon.station.realtime.message;

import com.ganzithon.station.realtime.store.BroadcastStore.ReactionCounts;

/**
 * 방 전체 (/topic/rooms/{roomCode}): 1초 리액션 (BR-03). 저장하지 않고 바로 전파만 합니다 (알림 없음).
 * 프론트는 ticketNumber(반응한 양) 캐릭터 옆에 이모지를 띄우고, 전광판의 끄덕끄덕·전해요 숫자를 nodCount·sendCount로 바꿉니다.
 *
 * @param broadcastId  반응한 전광판 방송
 * @param ticketNumber 반응한 승객
 * @param reaction     NOD(끄덕끄덕) 또는 SEND(전해요)
 * @param nodCount     그 방송에 끄덕끄덕한 승객 수 (한 명당 1번)
 * @param sendCount    그 방송에 전해요를 누른 승객 수 (한 명당 1번)
 */
public record ReactionMessage(
        MessageType type,
        String broadcastId,
        int ticketNumber,
        Reaction reaction,
        int nodCount,
        int sendCount,
        long serverTs
) {
    public static ReactionMessage of(String broadcastId, int ticketNumber, Reaction reaction,
                                     ReactionCounts counts, long serverTs) {
        return new ReactionMessage(MessageType.REACTION, broadcastId, ticketNumber, reaction,
                counts.nodCount(), counts.sendCount(), serverTs);
    }
}
