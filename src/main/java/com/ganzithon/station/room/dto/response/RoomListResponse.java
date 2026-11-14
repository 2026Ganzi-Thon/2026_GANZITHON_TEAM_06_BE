package com.ganzithon.station.room.dto.response;

import com.ganzithon.station.room.entity.Room;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 목록은 data를 배열로 만들지 않고 필드로 감쌉니다: "data": { "rooms": [ ... ] }
 */
public record RoomListResponse(
        @Schema(description = "대기실 목록 (화면 표시 순서)")
        List<RoomResponse> rooms
) {
    public static RoomListResponse from(List<Room> rooms) {
        return new RoomListResponse(rooms.stream()
                .map(RoomResponse::from)
                .toList());
    }
}
