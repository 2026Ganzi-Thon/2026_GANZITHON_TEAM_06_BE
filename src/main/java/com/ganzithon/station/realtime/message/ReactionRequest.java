package com.ganzithon.station.realtime.message;

/**
 * 1초 리액션 (BR-03). SEND /app/rooms/{roomCode}/reaction
 * 기능명세 BR-03: 방송 직후 리액션 터치 (끄덕끄덕 / 전해요). 리액션은 전광판 방송에만 합니다.
 *
 * @param broadcastId 반응할 전광판 방송 (BROADCAST의 broadcastId). 필수
 * @param reaction    NOD(끄덕끄덕) 또는 SEND(전해요). 필수
 */
public record ReactionRequest(
        String broadcastId,
        Reaction reaction
) {
    public boolean isComplete() {
        return broadcastId != null && !broadcastId.isBlank() && reaction != null;
    }
}
