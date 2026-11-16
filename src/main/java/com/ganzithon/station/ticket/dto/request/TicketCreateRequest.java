package com.ganzithon.station.ticket.dto.request;

import com.ganzithon.station.room.entity.MoodChip;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 승차권 발급 (ON-03, ON-04). POST /api/v1/tickets
 * 토큰 없이 호출할 수 있습니다. 계정 토큰이 있으면 그 계정 소유, 없으면 게스트 토큰을 만들어 응답에 넣습니다.
 */
public record TicketCreateRequest(
        @Schema(description = "대기실 코드", example = "RESULT")
        @NotBlank(message = "대기실 코드를 보내야 합니다.")
        String roomCode,

        @Schema(description = "상태 칩. 건너뛰면 null (호칭이 \"104번 양\"이 됩니다)", example = "FLUTTERING", nullable = true)
        MoodChip moodChip
) {
}
