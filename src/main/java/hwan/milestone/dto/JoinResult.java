package hwan.milestone.dto;

import java.util.Optional;

/**
 * @param previousRoomLeave 다른 방에 있다가 옮겨 온 경우 이전 방에 보낼 LEAVE
 * @param join              새로 입장한 경우 방에 보낼 JOIN (같은 방 재요청이면 empty)
 * @param fullSnapshot      입장한 세션에게만 보낼 방 전체 목록
 */
public record JoinResult(Optional<RoomEvent> previousRoomLeave, Optional<RoomEvent> join, SnapshotEvent fullSnapshot) {
}
