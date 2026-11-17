package com.ganzithon.station.ticket.dto.request;

import com.ganzithon.station.ticket.entity.EndReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 기다림 종료 (EN-01, EN-02). POST /api/v1/tickets/{ticketId}/end
 * 회고 질문 답변(EN-03)은 저장하지 않아서 보내지 않습니다.
 */
public record TicketEndRequest(
        @Schema(description = "종료 사유. ANSWERED(답이 왔어요), UNEXPECTED(기대와 달랐어요), LET_GO(내려놓을래요)",
                example = "ANSWERED")
        @NotNull(message = "종료 사유를 보내야 합니다.")
        EndReason endReason
) {
}
