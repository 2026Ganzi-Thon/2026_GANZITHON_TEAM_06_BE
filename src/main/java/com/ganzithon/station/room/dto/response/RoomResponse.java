package com.ganzithon.station.room.dto.response;

import com.ganzithon.station.room.entity.Room;
import io.swagger.v3.oas.annotations.media.Schema;

public record RoomResponse(
        @Schema(description = "대기실 코드. 단건 조회 경로에 씁니다", example = "RESULT")
        String roomCode,

        @Schema(description = "대기실 이름", example = "결과발표")
        String name,

        @Schema(description = "대기실 상단 슬로건", example = "결과를 기다리는 자리, 잠시 마음을 가만히 내려놓아도 돼요.")
        String slogan,

        @Schema(description = "회고 질문 1. 아직 없으면 null", example = "기다리는 동안 어떤 마음이었나요?", nullable = true)
        String retrospectQuestion1,

        @Schema(description = "회고 질문 2. 아직 없으면 null", example = "지금 나에게 해 주고 싶은 말은?", nullable = true)
        String retrospectQuestion2,

        @Schema(description = "NPC 공감 문구. 아직 없으면 null", example = "괜찮아요, 천천히 기다려요.", nullable = true)
        String npcEmpathyCopy
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getCode(),
                room.getName(),
                room.getSlogan(),
                room.getRetrospectQuestion1(),
                room.getRetrospectQuestion2(),
                room.getNpcEmpathyCopy()
        );
    }
}
