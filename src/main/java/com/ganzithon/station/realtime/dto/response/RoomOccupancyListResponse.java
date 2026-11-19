package com.ganzithon.station.realtime.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 목록은 data를 배열로 만들지 않고 필드로 감쌉니다: "data": { "rooms": [ ... ] }
 */
public record RoomOccupancyListResponse(
        @Schema(description = "대기실별 현재 인원 (대기실 목록과 같은 순서)")
        List<RoomOccupancyResponse> rooms
) {
}
