package com.ganzithon.station.global.config;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 응답 JSON의 {@link Instant}를 한국 시각 + 오프셋으로 내보냅니다 (docs/API_CONVENTION.md 5장).
 * 예: 2026-11-11T12:30:00Z → "2026-11-11T21:30:00+09:00"
 * DTO 필드는 Instant 그대로 두면 됩니다. WebSocket의 serverTs는 epoch ms(long)라 영향이 없습니다.
 */
@JacksonComponent
public class InstantJacksonSerializer extends ValueSerializer<Instant> {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(DISPLAY_ZONE);

    @Override
    public void serialize(Instant value, JsonGenerator generator, SerializationContext context) {
        generator.writeString(FORMATTER.format(value));
    }
}
