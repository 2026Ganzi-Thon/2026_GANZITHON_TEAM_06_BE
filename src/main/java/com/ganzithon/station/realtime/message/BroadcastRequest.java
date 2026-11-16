package com.ganzithon.station.realtime.message;

import com.ganzithon.station.room.entity.MoodChip;

/**
 * 조합형 방송 (BR-01). SEND /app/rooms/{roomCode}/broadcast
 *
 * @param situationChipCode 그 대기실의 상황 칩 코드 (GET /api/v1/rooms/{roomCode}/situation-chips)
 * @param moodChip          상태 칩 (필수). 고르면 대기실 호칭이 이 칩으로 바뀝니다
 */
public record BroadcastRequest(
        String situationChipCode,
        MoodChip moodChip
) {
    public boolean isComplete() {
        return situationChipCode != null && !situationChipCode.isBlank() && moodChip != null;
    }
}
