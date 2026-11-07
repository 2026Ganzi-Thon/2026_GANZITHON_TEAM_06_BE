package hwan.milestone.domain;

import hwan.milestone.dto.PlayerState;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 메모리 상의 플레이어. 좌표는 불변 Position 으로 통째로 교체해 (x, y, seq) 일관성을 지킨다.
 */
public class Player {

    @Getter
    private final String id;
    @Getter
    private final String roomId;
    private volatile Position position;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public Player(String id, String roomId, double x, double y) {
        this.id = id;
        this.roomId = roomId;
        this.position = new Position(x, y, 0);
    }

    /** 최신 좌표만 저장하고 dirty 플래그를 세운다. (쓰기 순서: position -> dirty) */
    public void moveTo(double x, double y, long seq) {
        this.position = new Position(x, y, seq);
        this.dirty.set(true);
    }

    /** dirty 였다면 플래그를 내리고 true 를 반환한다. 틱 브로드캐스터만 호출한다. */
    public boolean consumeDirty() {
        return dirty.getAndSet(false);
    }

    public PlayerState toState() {
        Position p = position;
        return new PlayerState(id, p.x(), p.y(), p.seq());
    }

    private record Position(double x, double y, long seq) {
    }
}
