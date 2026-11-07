package hwan.milestone.dto;

/** 서버 -> 클라이언트 메시지 구분자. 모든 서버 발신 메시지는 type 필드를 가진다. */
public enum MessageType {
    JOIN, LEAVE, SNAPSHOT, BROADCAST, REACTION, ERROR
}
