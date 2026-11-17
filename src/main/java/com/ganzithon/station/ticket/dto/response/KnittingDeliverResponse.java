package com.ganzithon.station.ticket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 털실 톡톡 "역장에게 전달" 결과.
 */
public record KnittingDeliverResponse(
        @Schema(description = "전달한 시각. 이미 전달했으면 처음 전달한 시각", example = "2026-11-17T21:33:00+09:00")
        Instant knittingDeliveredAt
) {
}
