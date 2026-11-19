package com.ganzithon.station.realtime.service;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.realtime.message.ReactionMessage;
import com.ganzithon.station.realtime.message.ReactionRequest;
import com.ganzithon.station.realtime.messaging.RoomEvent;
import com.ganzithon.station.realtime.store.BroadcastStore;
import com.ganzithon.station.realtime.store.Passenger;
import com.ganzithon.station.realtime.store.ReactionStore;
import com.ganzithon.station.realtime.store.RoomPassengerStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 1초 리액션 (BR-03, 담당: 환희). 전광판 방송에 끄덕끄덕·전해요로 반응하고, DB에 남기지 않고 방에 바로 전파합니다.
 * 리액션은 가벼운 동작이라 연타·지난 방송 같은 경우는 에러 없이 조용히 버립니다.
 */
@Service
@RequiredArgsConstructor
public class ReactionService {

    private final RoomPassengerStore roomPassengerStore;
    private final BroadcastStore broadcastStore;
    private final ReactionStore reactionStore;

    /**
     * 방에 보낼 REACTION (그 방송의 끄덕끄덕·전해요 수 포함). 아래 경우에는 empty (보내지 않음)
     * <ul>
     *     <li>broadcastId가 지금 전광판에 나오는 방송이 아님 (이미 내려갔거나 없는 방송)</li>
     *     <li>1초 안에 또 보냄 (승차권 기준)</li>
     * </ul>
     *
     * @throws BusinessException COMMON_INVALID_INPUT broadcastId·reaction 누락, REALTIME_NOT_JOINED 그 방에 입장 안 함
     */
    public Optional<RoomEvent> react(String roomCode, String sessionId, ReactionRequest request) {
        if (request == null || !request.isComplete()) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
        }
        Passenger passenger = roomPassengerStore.findBySessionId(sessionId)
                .filter(found -> found.roomCode().equals(roomCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.REALTIME_NOT_JOINED));

        long now = System.currentTimeMillis();
        if (!broadcastStore.isOnAir(roomCode, request.broadcastId(), now)
                || !reactionStore.tryAcquire(passenger.ticketId(), now)) {
            return Optional.empty();
        }
        // 위 확인과 여기 사이에 방송이 내려갔으면 보내지 않습니다
        return broadcastStore.countReaction(roomCode, request.broadcastId(), passenger.ticketId(), request.reaction(), now)
                .map(counts -> new RoomEvent(roomCode, ReactionMessage.of(
                        request.broadcastId(), passenger.ticketNumber(), request.reaction(), counts, now)));
    }
}
