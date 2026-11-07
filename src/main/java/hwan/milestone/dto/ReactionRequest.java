package hwan.milestone.dto;

/** 클라이언트 -> 서버. 여기서 type 은 리액션 종류(NOD | AWW)다. */
public record ReactionRequest(String broadcastId, ReactionType type) {
}
