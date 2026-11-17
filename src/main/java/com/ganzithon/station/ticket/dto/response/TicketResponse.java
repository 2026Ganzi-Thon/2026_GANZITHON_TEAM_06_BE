package com.ganzithon.station.ticket.dto.response;

import com.ganzithon.station.global.config.TrainProperties;
import com.ganzithon.station.room.entity.MoodChip;
import com.ganzithon.station.ticket.entity.EndReason;
import com.ganzithon.station.ticket.entity.Ticket;
import com.ganzithon.station.ticket.entity.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 내 승차권 상태. 새로고침했을 때 화면(열차, 털실, 털실 톡톡 전달 표시)을 다시 그리는 데 씁니다.
 */
public record TicketResponse(
        @Schema(description = "승차권 ID", example = "1")
        Long ticketId,

        @Schema(description = "대기실 코드", example = "RESULT")
        String roomCode,

        @Schema(description = "승차권 번호", example = "104")
        int ticketNumber,

        @Schema(description = "대기실 호칭", example = "104번 두근두근한 양")
        String displayName,

        @Schema(description = "상태 칩. 없으면 null", example = "FLUTTERING", nullable = true)
        MoodChip moodChip,

        @Schema(description = "털실 톡톡 도안 코드. 도안을 바꿨으면 바꾼 도안", example = "PATTERN_02")
        String patternCode,

        @Schema(description = "WAITING(기다리는 중) 또는 ENDED(종료)", example = "WAITING")
        TicketStatus status,

        @Schema(description = "종료 사유. 진행 중이면 null", example = "ANSWERED", nullable = true)
        EndReason endReason,

        @Schema(description = "기다림 시작 시각", example = "2026-11-17T21:30:00+09:00")
        Instant startedAt,

        @Schema(description = "기다림 종료 시각. 진행 중이면 null", nullable = true)
        Instant endedAt,

        @Schema(description = "서버 현재 시각. 기기 시계가 틀려도 경과 시간을 맞게 계산하려고 함께 보냅니다",
                example = "2026-11-17T21:33:00+09:00")
        Instant serverTime,

        @Schema(description = "공용 열차 간격(ms). 모든 승객이 같은 시각표: epoch부터 이 값마다 1대 (30분이면 매시 00분·30분)", example = "1800000")
        long trainIntervalMs,

        @Schema(description = "내 첫 열차 시각 = 시작 + 10초 (TR-01). 이 열차는 내 화면에만 지나갑니다", example = "2026-11-17T21:30:10+09:00")
        Instant firstTrainAt,

        @Schema(description = "다음 열차 시각. 내 첫 열차가 아직이면 그 시각, 아니면 다음 공용 열차", example = "2026-11-17T22:00:00+09:00")
        Instant nextTrainAt,

        @Schema(description = "지금까지 지나간 열차 수 (= 받은 털실 수). 끝났으면 종료 시각 기준", example = "12")
        int trainCount,

        @Schema(description = "남은 털실 수 (받은 털실 - 도안 바꾸기에 쓴 털실)", example = "11")
        int remainingYarnCount,

        @Schema(description = "털실 톡톡 \"역장에게 전달\" 시각. 있으면 털실 보따리 아이콘을 보여 주세요. 도안을 바꾸면 지워집니다",
                nullable = true)
        Instant knittingDeliveredAt
) {
    public static TicketResponse of(Ticket ticket, Instant serverTime, TrainProperties train) {
        Instant until = ticket.countingUntil(serverTime);
        return new TicketResponse(
                ticket.getId(),
                ticket.getRoom().getCode(),
                ticket.getTicketNumber(),
                ticket.getDisplayName(),
                ticket.getMoodChip(),
                ticket.getPattern().getCode(),
                ticket.getStatus(),
                ticket.getEndReason(),
                ticket.getStartedAt(),
                ticket.getEndedAt(),
                serverTime,
                train.interval().toMillis(),
                ticket.firstTrainAt(train),
                ticket.nextTrainAt(serverTime, train),
                ticket.countPassedTrains(until, train),
                ticket.countRemainingYarn(until, train),
                ticket.getKnittingDeliveredAt());
    }
}
