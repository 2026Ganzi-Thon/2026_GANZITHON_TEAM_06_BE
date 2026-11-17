package com.ganzithon.station.ticket.event;

import com.ganzithon.station.ticket.entity.EndReason;

/**
 * 기다림이 종료됐음. TicketService가 발행하고, 받는 쪽은 커밋된 뒤에만 처리합니다 ({@code @TransactionalEventListener}).
 * <ul>
 *     <li>ticket: 전광판 통계 카운터 올리기 ({@code TicketStatsCounter})</li>
 *     <li>realtime: 대기실에서 그 승객 내보내기 (LEAVE)</li>
 * </ul>
 * ticket이 realtime을 직접 부르지 않으려고 이벤트로 알립니다 (호출 방향: realtime → ticket).
 *
 * @param ticketId  종료한 승차권
 * @param roomCode  그 승차권의 대기실
 * @param endReason 종료 사유
 */
public record TicketEndedEvent(
        Long ticketId,
        String roomCode,
        EndReason endReason
) {
}
