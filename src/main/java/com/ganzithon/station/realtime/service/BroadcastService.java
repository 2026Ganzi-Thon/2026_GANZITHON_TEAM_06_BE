package com.ganzithon.station.realtime.service;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.realtime.message.BroadcastAcceptedMessage;
import com.ganzithon.station.realtime.message.BroadcastMessage;
import com.ganzithon.station.realtime.message.BroadcastRequest;
import com.ganzithon.station.realtime.messaging.RoomEvent;
import com.ganzithon.station.realtime.store.BroadcastStore;
import com.ganzithon.station.realtime.store.Passenger;
import com.ganzithon.station.realtime.store.QueuedBroadcast;
import com.ganzithon.station.realtime.store.RoomPassengerStore;
import com.ganzithon.station.ticket.dto.response.BroadcastRecordResponse;
import com.ganzithon.station.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 조합형 방송(BR-01)과 전광판 송출(BR-02, 담당: 환희). 대기열과 쿨다운은 메모리에만 있습니다.
 * 호출 방향: realtime → ticket (칩 확인과 기록).
 */
@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final TicketService ticketService;
    private final RoomPassengerStore roomPassengerStore;
    private final BroadcastStore broadcastStore;

    /**
     * 방송 요청. 통과하면 대기열에 넣고, 실제 송출은 {@link #collectDue()}가 합니다.
     *
     * @throws BusinessException COMMON_INVALID_INPUT 칩 누락, REALTIME_NOT_JOINED 그 방에 입장 안 함,
     *                           BROADCAST_QUEUE_FULL, BROADCAST_COOLDOWN(retryAfterMs),
     *                           SITUATION_CHIP_NOT_FOUND 그 방의 칩이 아님, TICKET_ALREADY_ENDED
     */
    public BroadcastAcceptedMessage submit(String roomCode, String sessionId, BroadcastRequest request) {
        if (request == null || !request.isComplete()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
        Passenger passenger = roomPassengerStore.findBySessionId(sessionId)
                .filter(found -> found.roomCode().equals(roomCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.REALTIME_NOT_JOINED));
        if (broadcastStore.isQueueFull(roomCode)) {
            throw new BusinessException(ErrorCode.BROADCAST_QUEUE_FULL);
        }

        long now = System.currentTimeMillis();
        long remainingMs = broadcastStore.tryAcquireCooldown(passenger.ticketId(), now);
        if (remainingMs > 0) {
            throw new BusinessException(ErrorCode.BROADCAST_COOLDOWN, remainingMs);
        }

        BroadcastRecordResponse record;
        try {
            record = ticketService.recordBroadcast(passenger.ticketId(), request.situationChipCode(), request.moodChip());
        } catch (RuntimeException e) {
            broadcastStore.releaseCooldown(passenger.ticketId(), now);
            throw e;
        }

        roomPassengerStore.updateMood(passenger.ticketId(), roomCode, record.displayName(), request.moodChip());
        String broadcastId = UUID.randomUUID().toString();
        int queuePosition = broadcastStore.enqueue(new QueuedBroadcast(broadcastId, roomCode, passenger.ticketNumber(),
                record.displayName(), request.moodChip(), request.situationChipCode(), record.situationChipLabel(), now));
        return BroadcastAcceptedMessage.of(broadcastId, queuePosition, broadcastStore.getCooldownMs(), record.displayName());
    }

    /** 지금 송출할 방송. BroadcastDispatcher가 주기적으로 부릅니다. */
    public List<RoomEvent> collectDue() {
        long now = System.currentTimeMillis();
        return broadcastStore.pollDue(now).stream()
                .map(broadcast -> new RoomEvent(broadcast.roomCode(),
                        BroadcastMessage.of(broadcast, broadcastStore.getDisplayMs(), now)))
                .toList();
    }
}
