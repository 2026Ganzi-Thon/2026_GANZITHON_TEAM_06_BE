package com.ganzithon.station.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * STOMP 목적지 허용 규칙 (Spring 없이).
 */
class StompDestinationRuleTest {

    @Test
    void SEND는_app_경로만_허용한다() {
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SEND, "/app/rooms/RESULT/broadcast")).isTrue();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SEND, "/topic/rooms/RESULT")).isFalse();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SEND, "/queue/events")).isFalse();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SEND, "/user/abc/queue/events")).isFalse();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SEND, "/apps/x")).isFalse();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SEND, null)).isFalse();
    }

    @Test
    void SUBSCRIBE는_방_토픽과_내_개인_큐만_허용한다() {
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SUBSCRIBE, "/topic/rooms/RESULT")).isTrue();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SUBSCRIBE, "/user/queue/events")).isTrue();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SUBSCRIBE, "/queue/events-user123")).isFalse();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SUBSCRIBE, "/topic/other")).isFalse();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SUBSCRIBE, "/app/rooms/RESULT/join")).isFalse();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.SUBSCRIBE, null)).isFalse();
    }

    @Test
    void 목적지가_없는_명령은_통과한다() {
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.CONNECT, null)).isTrue();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.UNSUBSCRIBE, null)).isTrue();
        assertThat(StompAuthChannelInterceptor.isAllowedDestination(StompCommand.DISCONNECT, null)).isTrue();
    }
}
