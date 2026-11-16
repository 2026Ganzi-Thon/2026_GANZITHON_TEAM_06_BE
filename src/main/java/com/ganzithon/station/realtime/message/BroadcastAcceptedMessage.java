package com.ganzithon.station.realtime.message;

/**
 * 나에게만 (/user/queue/events): 방송 요청이 대기열에 들어감. 실제 송출은 방 전체로 오는 BROADCAST입니다.
 *
 * @param broadcastId   이 방송의 ID. 나중에 오는 BROADCAST와 같은 값
 * @param queuePosition 대기 순번 (1이면 다음 차례)
 * @param cooldownMs    다시 방송할 수 있을 때까지 (ms)
 * @param displayName   바뀐 내 대기실 호칭
 */
public record BroadcastAcceptedMessage(
        MessageType type,
        String broadcastId,
        int queuePosition,
        long cooldownMs,
        String displayName,
        long serverTs
) {
    public static BroadcastAcceptedMessage of(String broadcastId, int queuePosition, long cooldownMs, String displayName) {
        return new BroadcastAcceptedMessage(MessageType.BROADCAST_ACCEPTED, broadcastId, queuePosition, cooldownMs,
                displayName, System.currentTimeMillis());
    }
}
