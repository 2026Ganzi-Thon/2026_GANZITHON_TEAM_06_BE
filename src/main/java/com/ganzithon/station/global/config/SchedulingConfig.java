package com.ganzithon.station.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 주기 작업(@Scheduled) 켜기. 지금은 방송 송출(realtime BroadcastDispatcher, 500ms) 하나입니다.
 * 주기 작업을 추가하려면 먼저 공유해 주세요 (docs/CODE_CONVENTION.md 5장). 시간이 지나서 생기는 값(열차, 털실)은 스케줄러로 만들지 않습니다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
