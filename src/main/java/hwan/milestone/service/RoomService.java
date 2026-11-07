package hwan.milestone.service;

import hwan.milestone.domain.Player;
import hwan.milestone.dto.JoinEvent;
import hwan.milestone.dto.JoinResult;
import hwan.milestone.dto.LeaveEvent;
import hwan.milestone.dto.MoveRequest;
import hwan.milestone.dto.PlayerState;
import hwan.milestone.dto.RoomEvent;
import hwan.milestone.dto.SnapshotEvent;
import hwan.milestone.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RoomService {

    public static final double MAP_WIDTH = 800;
    public static final double MAP_HEIGHT = 600;
    private static final double SPAWN_MARGIN = 40;
    private static final Pattern ROOM_ID = Pattern.compile("[A-Za-z0-9_-]{1,40}");

    private final PlayerRepository playerRepository;

    public JoinResult join(String roomId, String sessionId) {
        if (!ROOM_ID.matcher(roomId).matches()) {
            throw new GameException("INVALID_ROOM", "roomId 는 영문/숫자/-/_ 1~40자여야 합니다.");
        }
        Optional<Player> existing = playerRepository.findBySessionId(sessionId);
        if (existing.isPresent() && existing.get().getRoomId().equals(roomId)) {
            // 같은 방 재요청: 전체 목록만 다시 보내 준다.
            return new JoinResult(Optional.empty(), Optional.empty(), fullSnapshot(roomId, sessionId));
        }
        Optional<RoomEvent> previousLeave = existing.isPresent() ? leave(sessionId) : Optional.empty();

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Player player = new Player(sessionId, roomId,
                rnd.nextDouble(SPAWN_MARGIN, MAP_WIDTH - SPAWN_MARGIN),
                rnd.nextDouble(SPAWN_MARGIN, MAP_HEIGHT - SPAWN_MARGIN));
        int count = playerRepository.save(player);

        return new JoinResult(
                previousLeave,
                Optional.of(new RoomEvent(roomId, JoinEvent.of(player.toState(), count))),
                fullSnapshot(roomId, sessionId));
    }

    public Optional<RoomEvent> leave(String sessionId) {
        return playerRepository.deleteBySessionId(sessionId)
                .map(r -> new RoomEvent(r.player().getRoomId(), LeaveEvent.of(sessionId, r.remaining())));
    }

    /** 즉시 브로드캐스트하지 않고 최신 좌표만 저장 + dirty 표시. */
    public void move(String roomId, String sessionId, MoveRequest req) {
        Player player = requireMember(roomId, sessionId);
        if (req == null || req.x() == null || req.y() == null || req.seq() == null
                || !Double.isFinite(req.x()) || !Double.isFinite(req.y())) {
            throw new GameException("BAD_REQUEST", "move 에는 x, y, seq 가 필요합니다.");
        }
        player.moveTo(clamp(req.x(), MAP_WIDTH), clamp(req.y(), MAP_HEIGHT), req.seq());
    }

    public Player requireMember(String roomId, String sessionId) {
        return playerRepository.findBySessionId(sessionId)
                .filter(p -> p.getRoomId().equals(roomId))
                .orElseThrow(() -> new GameException("NOT_JOINED", "먼저 방에 입장해야 합니다: " + roomId));
    }

    /** 틱마다 호출: 방별로 dirty 플레이어만 모아 SNAPSHOT 하나씩. 바뀐 게 없는 방은 제외. */
    public List<RoomEvent> collectDirtySnapshots() {
        long serverTs = System.currentTimeMillis();
        List<RoomEvent> events = new ArrayList<>();
        playerRepository.forEachRoom((roomId, players) -> {
            List<PlayerState> changed = null;
            for (Player p : players) {
                if (p.consumeDirty()) {
                    if (changed == null) {
                        changed = new ArrayList<>();
                    }
                    changed.add(p.toState());
                }
            }
            if (changed != null) {
                events.add(new RoomEvent(roomId, SnapshotEvent.delta(serverTs, changed)));
            }
        });
        return events;
    }

    private SnapshotEvent fullSnapshot(String roomId, String selfId) {
        return SnapshotEvent.full(selfId, playerRepository.findByRoomId(roomId).stream().map(Player::toState).toList());
    }

    private static double clamp(double v, double max) {
        return Math.max(0, Math.min(max, v));
    }
}
