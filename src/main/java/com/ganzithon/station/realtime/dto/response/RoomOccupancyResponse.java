package com.ganzithon.station.realtime.dto.response;

import com.ganzithon.station.realtime.store.RoomPassengerStore.Occupancy;
import io.swagger.v3.oas.annotations.media.Schema;

public record RoomOccupancyResponse(
        @Schema(description = "대기실 코드", example = "RESULT")
        String roomCode,

        @Schema(description = "지금 접속한 승객 수 (착석 + 입석). 가상 승객은 세지 않습니다", example = "12")
        int count,

        @Schema(description = "앉아 있는 승객 수", example = "12")
        int seatedCount,

        @Schema(description = "자리가 다 차서 서서 기다리는 승객 수", example = "0")
        int standingCount,

        @Schema(description = "이 대기실의 좌석 수", example = "30")
        int seatCount
) {
    public static RoomOccupancyResponse of(String roomCode, Occupancy occupancy, int seatCount) {
        return new RoomOccupancyResponse(roomCode, occupancy.seatedCount() + occupancy.standingCount(),
                occupancy.seatedCount(), occupancy.standingCount(), seatCount);
    }
}
