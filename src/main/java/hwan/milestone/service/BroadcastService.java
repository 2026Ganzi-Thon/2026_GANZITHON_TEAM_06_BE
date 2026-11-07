package hwan.milestone.service;

import hwan.milestone.domain.QueuedBroadcast;
import hwan.milestone.dto.BroadcastEvent;
import hwan.milestone.dto.BroadcastRequest;
import hwan.milestone.dto.MessageType;
import hwan.milestone.dto.ReactionEvent;
import hwan.milestone.dto.ReactionRequest;
import hwan.milestone.dto.RoomEvent;
import hwan.milestone.repository.BroadcastRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 조합형 방송(대기열 + 쿨다운)과 1초 리액션. */
@Service
@RequiredArgsConstructor
public class BroadcastService {

    public static final long COOLDOWN_MS = 30_000;
    public static final long DISPATCH_INTERVAL_MS = 15_000;
    /** 대기열 상한: 상한이 없으면 30초 쿨다운 x 다수 인원 > 15초당 1건 송출이라 대기열이 끝없이 늘어난다. */
    public static final int MAX_QUEUE_SIZE = 20;

    private final BroadcastRepository broadcastRepository;
    private final RoomService roomService;
    private final ChipCatalog chipCatalog;

    public void submit(String roomId, String sessionId, BroadcastRequest req) {
        roomService.requireMember(roomId, sessionId);
        if (req == null || req.situationChipId() == null || req.stateChipId() == null
                || chipCatalog.situationLabel(roomId, req.situationChipId()).isEmpty()
                || chipCatalog.stateLabel(req.stateChipId()).isEmpty()) {
            throw new GameException("INVALID_CHIP", "이 방에서 사용할 수 없는 칩입니다.");
        }
        if (broadcastRepository.queueSize(roomId) >= MAX_QUEUE_SIZE) {
            throw new GameException("QUEUE_FULL", "방송 대기열이 가득 찼습니다. 잠시 후 다시 시도해 주세요.");
        }
        long now = System.currentTimeMillis();
        long remaining = broadcastRepository.tryAcquireCooldown(sessionId, now, COOLDOWN_MS);
        if (remaining > 0) {
            throw new GameException("COOLDOWN", "방송은 30초에 한 번만 할 수 있습니다.", remaining);
        }
        broadcastRepository.enqueue(new QueuedBroadcast(
                UUID.randomUUID().toString(), roomId, sessionId,
                req.situationChipId(), req.stateChipId(), now));
    }

    /** 방마다 마지막 송출 후 15초가 지났으면 대기열에서 하나씩 꺼낸다. */
    public List<RoomEvent> collectDue() {
        long now = System.currentTimeMillis();
        List<RoomEvent> events = new ArrayList<>();
        for (String roomId : broadcastRepository.roomsWithQueue()) {
            broadcastRepository.pollIfDue(roomId, now, DISPATCH_INTERVAL_MS)
                    .ifPresent(b -> events.add(new RoomEvent(roomId, toEvent(b, now))));
        }
        return events;
    }

    /** 즉시 전파, 저장하지 않음. */
    public RoomEvent react(String roomId, String sessionId, ReactionRequest req) {
        roomService.requireMember(roomId, sessionId);
        if (req == null || req.broadcastId() == null || req.broadcastId().isBlank() || req.type() == null) {
            throw new GameException("BAD_REQUEST", "reaction 에는 broadcastId 와 type(NOD|AWW) 이 필요합니다.");
        }
        return new RoomEvent(roomId, ReactionEvent.of(req.broadcastId(), sessionId, req.type()));
    }

    public void forget(String sessionId) {
        broadcastRepository.deleteCooldown(sessionId);
    }

    private BroadcastEvent toEvent(QueuedBroadcast b, long now) {
        return new BroadcastEvent(
                MessageType.BROADCAST,
                b.broadcastId(),
                b.senderId(),
                b.situationChipId(),
                chipCatalog.situationLabel(b.roomId(), b.situationChipId()).orElse(b.situationChipId()),
                b.stateChipId(),
                chipCatalog.stateLabel(b.stateChipId()).orElse(b.stateChipId()),
                b.queuedAt(),
                now);
    }
}
