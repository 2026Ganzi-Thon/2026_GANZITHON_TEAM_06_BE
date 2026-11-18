package com.ganzithon.station.blanket.dto.response;

import com.ganzithon.station.blanket.entity.BlanketPiece;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 담요 조각 목록 항목 (앞면만). 상세는 {@link BlanketPieceResponse}를 씁니다.
 */
public record BlanketPieceListItem(
        @Schema(description = "조각 ID", example = "1")
        Long blanketPieceId,

        @Schema(description = "앞면 도안 코드", example = "PATTERN_HEART")
        String patternCode,

        @Schema(description = "조각을 만든 시각", example = "2026-11-11T21:30:00+09:00")
        Instant createdAt
) {
    public static BlanketPieceListItem from(BlanketPiece blanketPiece) {
        return new BlanketPieceListItem(
                blanketPiece.getId(),
                blanketPiece.getPattern().getCode(),
                blanketPiece.getCreatedAt()
        );
    }
}
