package com.ganzithon.station.realtime.message;

import com.ganzithon.station.blanket.dto.response.PublicCardStatus;
import com.ganzithon.station.room.entity.MoodChip;
import com.ganzithon.station.ticket.dto.response.PassengerCardResponse;

import java.util.List;

/**
 * 나에게만 (/user/queue/events): 탭한 승객의 확대 카드 (MY-05). 닉네임·종료 표식은 넣지 않습니다.
 * 명세: 공개한 승객은 조각 도안·승차권 번호·지나간 열차 수, 비공개 승객은 머문 시간(+상태 칩)만.
 *
 * @param visibility   PUBLIC / NO_PIECE / PRIVATE / GUEST. PUBLIC이 아니면 머문 시간과 상태 칩만 보여 주세요
 * @param startedTs    기다림 시작 시각 (epoch ms). 머문 시간 계산용
 * @param patternCodes PUBLIC일 때만: 담요 조각 도안 (최신부터 최대 9개). 그 외 null
 * @param trainCount   PUBLIC일 때만: 지나간 열차 수. 그 외 null
 */
public record PassengerCardMessage(
        MessageType type,
        int ticketNumber,
        String displayName,
        MoodChip moodChip,
        long startedTs,
        PublicCardStatus visibility,
        List<String> patternCodes,
        Integer trainCount,
        long serverTs
) {
    public static PassengerCardMessage from(PassengerCardResponse card) {
        return new PassengerCardMessage(MessageType.PASSENGER_CARD, card.ticketNumber(), card.displayName(),
                card.moodChip(), card.startedAt().toEpochMilli(), card.visibility(), card.patternCodes(),
                card.trainCount(), System.currentTimeMillis());
    }
}
