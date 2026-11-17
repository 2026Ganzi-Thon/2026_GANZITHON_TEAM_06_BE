package com.ganzithon.station.realtime.message;

/**
 * 1초 리액션 (BR-03). SEND /app/rooms/{roomCode}/reaction
 * 기능명세: "방송 직후 끄덕임 / 짠함 터치". 리액션은 전광판 방송에만 합니다.
 *
 * @param broadcastId 반응할 전광판 방송 (BROADCAST의 broadcastId). 필수
 * @param reaction    NOD(끄덕임) 또는 AWW(짠함). 필수
 */
public record ReactionRequest(
        String broadcastId,
        Reaction reaction
) {
    public boolean isComplete() {
        return broadcastId != null && !broadcastId.isBlank() && reaction != null;
    }
}
