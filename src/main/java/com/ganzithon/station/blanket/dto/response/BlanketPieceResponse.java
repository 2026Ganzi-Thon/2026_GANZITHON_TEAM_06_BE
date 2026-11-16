package com.ganzithon.station.blanket.dto.response;

import com.ganzithon.station.blanket.entity.BlanketPiece;
import com.ganzithon.station.ticket.entity.EndReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 담요 조각 (본인용, 앞면 + 뒷면). 공개 카드(MY-05)에는 endReason을 넣으면 안 되므로 이 DTO를 쓰지 않습니다.
 */
public record BlanketPieceResponse(
        @Schema(description = "조각 ID", example = "1")
        Long blanketPieceId,

        @Schema(description = "기다림(승차권) ID", example = "104")
        Long ticketId,

        @Schema(description = "앞면 도안 코드", example = "PATTERN_01")
        String patternCode,

        @Schema(description = "뒷면: 승차권 번호", example = "104")
        int ticketNumber,

        @Schema(description = "뒷면: 기다림 이름", example = "면접 보고 기다리는 중")
        String waitingName,

        @Schema(description = "뒷면: 지나간 열차 수", example = "12")
        int trainCount,

        @Schema(description = "뒷면: 종료 표식 (본인에게만)", example = "ANSWERED")
        EndReason endReason,

        @Schema(description = "조각을 만든 시각", example = "2026-11-11T21:30:00+09:00")
        Instant createdAt,

        @Schema(description = "조각 ID (8장 결정 필드명)", example = "1")
        Long pieceId,

        @Schema(description = "앞면 도안 ID", example = "1")
        Long patternId
) {
    public static BlanketPieceResponse from(BlanketPiece blanketPiece) {
        return new BlanketPieceResponse(
                blanketPiece.getId(),
                blanketPiece.getTicket().getId(),
                blanketPiece.getPattern().getCode(),
                blanketPiece.getTicketNumber(),
                blanketPiece.getWaitingName(),
                blanketPiece.getTrainCount(),
                blanketPiece.getEndReason(),
                blanketPiece.getCreatedAt(),
                blanketPiece.getId(),
                blanketPiece.getPattern().getId()
        );
    }
}
