package com.ganzithon.station.ticket.dto.response;

import com.ganzithon.station.ticket.entity.EndReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 기다림 종료 결과 = 이번 기다림의 담요 조각 1칸 (앞면 도안 + 뒷면 기록).
 * 로그인 상태면 계정에 저장됐고(savedToAccount = true), 비로그인이면 저장하지 않았으니 프론트가 브라우저에 임시 보관합니다.
 */
public record TicketEndResponse(
        @Schema(description = "승차권 ID. 나중에 계정으로 옮길 때(POST /api/v1/tickets/claim) 이 값을 보냅니다", example = "1")
        Long ticketId,

        @Schema(description = "앞면 도안 코드", example = "PATTERN_01")
        String patternCode,

        @Schema(description = "뒷면: 승차권 번호", example = "104")
        int ticketNumber,

        @Schema(description = "뒷면: 기다림 이름. 마지막으로 방송한 상황 칩, 방송을 안 했으면 대기실 이름", example = "답장 기다리는 중")
        String waitingName,

        @Schema(description = "뒷면: 지나간 열차 수 (= 받은 털실 수)", example = "12")
        int trainCount,

        @Schema(description = "뒷면: 종료 표식 (본인에게만)", example = "ANSWERED")
        EndReason endReason,

        @Schema(description = "기다림 시작 시각", example = "2026-11-17T21:30:00+09:00")
        Instant startedAt,

        @Schema(description = "기다림 종료 시각", example = "2026-11-17T21:33:00+09:00")
        Instant endedAt,

        @Schema(description = "계정에 저장됐는지. false면 비로그인이라 저장하지 않았으니 브라우저에 임시 보관하세요", example = "false")
        boolean savedToAccount
) {
}
