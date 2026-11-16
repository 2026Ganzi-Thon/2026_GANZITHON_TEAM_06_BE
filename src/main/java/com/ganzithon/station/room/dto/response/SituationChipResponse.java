package com.ganzithon.station.room.dto.response;

import com.ganzithon.station.room.entity.SituationChip;
import io.swagger.v3.oas.annotations.media.Schema;

public record SituationChipResponse(
        @Schema(description = "상황 칩 코드. 방송 요청 때 이 값을 보냅니다", example = "NO_REPLY_READ_LONG_AGO")
        String situationChipCode,
        @Schema(description = "화면에 보여 줄 문구", example = "읽음 표시 뜬 지 한참째")
        String label
) {
    public static SituationChipResponse from(SituationChip situationChip) {
        return new SituationChipResponse(situationChip.getCode(), situationChip.getLabel());
    }
}
