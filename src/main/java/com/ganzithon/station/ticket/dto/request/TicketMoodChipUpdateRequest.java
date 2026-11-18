package com.ganzithon.station.ticket.dto.request;

import com.ganzithon.station.room.entity.MoodChip;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 승차권 상태 칩 바꾸기 (ON-03, 온보딩용). PATCH /api/v1/tickets/{ticketId}
 * S04에서 뒤로 가서 칩을 다시 고른 경우, 재발급 대신 같은 승차권(같은 번호)의 칩만 바꿉니다.
 */
public record TicketMoodChipUpdateRequest(
        @Schema(description = "새 상태 칩. null이거나 빠지면 칩 없이 \"104번 양\"이 됩니다", example = "TIRED", nullable = true)
        MoodChip moodChip
) {
}
