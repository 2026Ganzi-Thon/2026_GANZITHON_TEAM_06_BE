package hwan.milestone.dto;

/** 서비스 -> 전송 계층: 어느 방에 어떤 이벤트를 보낼지. */
public record RoomEvent(String roomId, Object payload) {
}
