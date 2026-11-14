package com.ganzithon.station.ticket.entity;

/**
 * 기다림 상태. WAITING → ENDED 한 방향만 있고, 이어서 재개는 없습니다 (다시 기다리려면 새 Ticket).
 */
public enum TicketStatus {
    WAITING,
    ENDED
}
