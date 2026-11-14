package com.ganzithon.station.ticket.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 임시 조각을 계정으로 옮기기 (MY-08). POST /api/v1/tickets/claim
 * 계정 토큰(Authorization) + X-Guest-Token 헤더가 함께 필요합니다.
 * 브라우저가 보관한 조각 내용은 보내지 않고 승차권 ID만 보냅니다 (서버가 Ticket 기록으로 다시 만듦).
 */
public record TicketClaimRequest(
        @Schema(description = "옮길 승차권 ID 목록 (브라우저에 임시 저장된 조각들)", example = "[104, 105]")
        @NotEmpty(message = "옮길 승차권을 하나 이상 보내야 합니다.")
        @Size(max = 50, message = "한 번에 50개까지 옮길 수 있습니다.")
        List<@NotNull Long> ticketIds
) {
}
