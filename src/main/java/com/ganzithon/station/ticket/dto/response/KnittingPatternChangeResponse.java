package com.ganzithon.station.ticket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 털실 톡톡 도안 바꾸기 결과. 바꾸면 처음부터 다시 채우고, 이전 "역장에게 전달" 기록도 지워집니다.
 */
public record KnittingPatternChangeResponse(
        @Schema(description = "새 도안 코드. 항상 바꾸기 전 도안과 다릅니다", example = "PATTERN_CLOUD")
        String patternCode,

        @Schema(description = "털실 1올을 쓰고 남은 털실 수", example = "4")
        int remainingYarnCount
) {
}
