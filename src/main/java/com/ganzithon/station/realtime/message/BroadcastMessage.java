package com.ganzithon.station.realtime.message;

import com.ganzithon.station.realtime.store.QueuedBroadcast;
import com.ganzithon.station.room.entity.MoodChip;

/**
 * 방 전체 (/topic/rooms/{roomCode}): 전광판 방송 (BR-02). 대기열에서 송출 간격마다 하나씩 나갑니다.
 * 보낸 승객의 카드 호칭도 이 메시지의 displayName·moodChip으로 갱신합니다.
 *
 * @param ticketNumber 보낸 승객
 * @param displayName  보낸 승객의 호칭 (방송 때 고른 상태 칩 반영)
 * @param sentence     전광판 문장. 예: 안내 말씀 드립니다. 104번 두근두근한 양님은 지금 '답장 기다리는 중'입니다.
 * @param displayMs    전광판에 보여 줄 시간 (ms)
 * @param queuedTs     방송을 요청한 시각 (epoch ms)
 */
public record BroadcastMessage(
        MessageType type,
        String broadcastId,
        int ticketNumber,
        String displayName,
        MoodChip moodChip,
        String situationChipCode,
        String situationChipLabel,
        String sentence,
        long displayMs,
        long queuedTs,
        long serverTs
) {
    private static final String SENTENCE_FORMAT = "안내 말씀 드립니다. %s님은 지금 '%s'입니다.";

    /** 전광판 문장. 예: 안내 말씀 드립니다. 104번 두근두근한 양님은 지금 '답장 기다리는 중'입니다. */
    public static String sentence(QueuedBroadcast broadcast) {
        return SENTENCE_FORMAT.formatted(broadcast.displayName(), broadcast.situationChipLabel());
    }

    public static BroadcastMessage of(QueuedBroadcast broadcast, long displayMs, long serverTs) {
        return new BroadcastMessage(
                MessageType.BROADCAST,
                broadcast.broadcastId(),
                broadcast.ticketNumber(),
                broadcast.displayName(),
                broadcast.moodChip(),
                broadcast.situationChipCode(),
                broadcast.situationChipLabel(),
                sentence(broadcast),
                displayMs,
                broadcast.queuedTs(),
                serverTs);
    }
}
