package com.ganzithon.station.room.dto.response;

import com.ganzithon.station.room.entity.SituationChip;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 목록은 data를 배열로 만들지 않고 필드로 감쌉니다: "data": { "situationChips": [ ... ] }
 */
public record SituationChipListResponse(
        @Schema(description = "그 대기실의 상황 칩 목록 (화면 표시 순서)")
        List<SituationChipResponse> situationChips
) {
    public static SituationChipListResponse from(List<SituationChip> situationChips) {
        return new SituationChipListResponse(situationChips.stream()
                .map(SituationChipResponse::from)
                .toList());
    }
}
