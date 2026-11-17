package com.ganzithon.station.blanket.dto.response;

import java.util.List;

/**
 * 대기실 공개 카드. REST가 아니라 realtime 패키지가 BlanketService를 직접 호출해서 받습니다.
 *
 * @param card status가 PUBLIC일 때만 채워지는 도안 코드 목록 (최신 조각부터, 최대 {@code BlanketService.MAX_VISIBLE_PIECE_COUNT}개)
 */
public record PublicCardResponse(
        PublicCardStatus status,
        String message,
        List<String> card
) {
}
