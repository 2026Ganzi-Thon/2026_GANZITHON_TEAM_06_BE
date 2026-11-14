package com.ganzithon.station.ticket.dto.response;

import com.ganzithon.station.blanket.dto.response.BlanketPieceResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record TicketClaimResponse(
        @Schema(description = "계정에 새로 저장된 담요 조각. 프론트는 이 승차권들의 임시 조각을 브라우저에서 지웁니다")
        List<BlanketPieceResponse> blanketPieces
) {
}
