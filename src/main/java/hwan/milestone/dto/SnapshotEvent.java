package hwan.milestone.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 틱 SNAPSHOT: 바뀐(dirty) 플레이어만 담는다.
 * 입장 직후 개인에게 보내는 전체 목록은 full=true, selfId 가 추가되고 방 전체 플레이어를 담는다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SnapshotEvent(MessageType type, long serverTs, List<PlayerState> players, Boolean full, String selfId) {

    public static SnapshotEvent delta(long serverTs, List<PlayerState> players) {
        return new SnapshotEvent(MessageType.SNAPSHOT, serverTs, players, null, null);
    }

    public static SnapshotEvent full(String selfId, List<PlayerState> players) {
        return new SnapshotEvent(MessageType.SNAPSHOT, System.currentTimeMillis(), players, true, selfId);
    }
}
