package com.ganzithon.station.blanket.dto.request;

import com.ganzithon.station.ticket.entity.EndReason;

/**
 * 담요 조각 저장 요청. HTTP body가 아니라 TicketService → BlanketService로 넘기는 값입니다.
 * 값은 모두 ticket이 Ticket 기록으로 계산해서 채웁니다. blanket은 다시 계산하지 않고 그대로 저장합니다.
 *
 * @param ticketId     조각을 만든 기다림 (조각 1칸 = 기다림 1건)
 * @param userId       조각을 가질 계정
 * @param patternId    앞면 도안 (발급 때 배정, 도안 바꾸기로 바뀌었으면 마지막 도안)
 * @param ticketNumber 뒷면 기록: 승차권 번호
 * @param waitingName  뒷면 기록: 마지막 방송 상황 칩 문구, 방송을 안 했으면 방 이름
 * @param trainCount   뒷면 기록: 지나간 열차 수
 * @param endReason    뒷면 기록: 종료 표식
 */
public record BlanketPieceCreateRequest(
        Long ticketId,
        Long userId,
        Long patternId,
        int ticketNumber,
        String waitingName,
        int trainCount,
        EndReason endReason
) {
}
