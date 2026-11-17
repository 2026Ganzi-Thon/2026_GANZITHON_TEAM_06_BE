package com.ganzithon.station.ticket.dto.response;

import com.ganzithon.station.global.config.TrainProperties;
import com.ganzithon.station.room.entity.MoodChip;
import com.ganzithon.station.ticket.entity.Ticket;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record TicketCreateResponse(
        @Schema(description = "승차권 ID. 이후 승차권 API와 대기실 입장에 씁니다", example = "1")
        Long ticketId,

        @Schema(description = "대기실 코드", example = "RESULT")
        String roomCode,

        @Schema(description = "승차권 번호. 대기실마다 1번부터 매깁니다", example = "104")
        int ticketNumber,

        @Schema(description = "대기실 호칭", example = "104번 두근두근한 양")
        String displayName,

        @Schema(description = "상태 칩. 건너뛰었으면 null", example = "FLUTTERING", nullable = true)
        MoodChip moodChip,

        @Schema(description = "털실 톡톡 도안 코드 (발급 때 랜덤 배정)", example = "PATTERN_01")
        String patternCode,

        @Schema(description = "게스트 토큰. 비로그인 발급일 때만 있고, 이후 요청에 X-Guest-Token 헤더로 보냅니다. 로그인 발급이면 null",
                example = "Qm9yZWQ...43자", nullable = true)
        String guestToken,

        @Schema(description = "기다림 시작 시각", example = "2026-11-16T21:30:00+09:00")
        Instant startedAt,

        @Schema(description = "서버 현재 시각. 기기 시계가 틀려도 경과 시간을 맞게 계산하려고 함께 보냅니다",
                example = "2026-11-16T21:30:00+09:00")
        Instant serverTime,

        @Schema(description = "공용 열차 간격(ms). 모든 승객이 같은 시각표: epoch부터 이 값마다 1대 (30분이면 매시 00분·30분)",
                example = "1800000")
        long trainIntervalMs,

        @Schema(description = "내 첫 열차 시각 = 시작 + 10초 (TR-01). 이 열차는 내 화면에만 지나갑니다", example = "2026-11-16T21:30:10+09:00")
        Instant firstTrainAt,

        @Schema(description = "다음 열차 시각. 내 첫 열차가 아직이면 그 시각, 아니면 다음 공용 열차. "
                + "내 열차 수(= 털실 수) = 내 첫 열차 1대 + 시작 뒤에 지나간 공용 열차", example = "2026-11-16T21:30:10+09:00")
        Instant nextTrainAt
) {
    public static TicketCreateResponse of(Ticket ticket, Instant serverTime, TrainProperties train) {
        return new TicketCreateResponse(
                ticket.getId(),
                ticket.getRoom().getCode(),
                ticket.getTicketNumber(),
                ticket.getDisplayName(),
                ticket.getMoodChip(),
                ticket.getPattern().getCode(),
                ticket.getGuestToken(),
                ticket.getStartedAt(),
                serverTime,
                train.interval().toMillis(),
                ticket.firstTrainAt(train),
                ticket.nextTrainAt(serverTime, train));
    }
}
