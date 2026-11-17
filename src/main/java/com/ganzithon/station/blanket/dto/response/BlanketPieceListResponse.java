package com.ganzithon.station.blanket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 내 담요 조각 목록. 배열을 바로 내려주지 않고 필드로 감쌉니다.
 */
public record BlanketPieceListResponse(
        @Schema(description = "최신 순(만든 시각 내림차순) 담요 조각, 최대 9개")
        List<BlanketPieceListItem> blanketPieces
) {
}
