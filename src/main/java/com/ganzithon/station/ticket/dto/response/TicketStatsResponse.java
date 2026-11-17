package com.ganzithon.station.ticket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 전광판 통계 (BR-04). 모든 대기실 합산입니다. 어떻게 묶어 보여 줄지는 프론트가 정합니다.
 */
public record TicketStatsResponse(
        @Schema(description = "ANSWERED(답이 왔어요)로 끝난 기다림 수", example = "12")
        long answeredCount,

        @Schema(description = "UNEXPECTED(기대와 달랐어요)로 끝난 기다림 수", example = "3")
        long unexpectedCount,

        @Schema(description = "LET_GO(내려놓을래요)로 끝난 기다림 수", example = "5")
        long letGoCount
) {
}
