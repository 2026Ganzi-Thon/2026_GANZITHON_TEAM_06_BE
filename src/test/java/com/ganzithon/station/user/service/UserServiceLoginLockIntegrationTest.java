package com.ganzithon.station.user.service;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.user.dto.request.UserLoginRequest;
import com.ganzithon.station.user.dto.request.UserSignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 로그인 실패 횟수는 트랜잭션이 실패(BusinessException)로 끝나도 커밋되어야 합니다.
 * Mockito 테스트는 롤백이 없어 이 경로를 확인하지 못하므로 실제 Postgres로 확인합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserServiceLoginLockIntegrationTest {

    @Autowired
    UserService userService;

    @Test
    void PIN을_5번_연속_틀리면_잠금이_DB에도_반영된다() {
        userService.signup(new UserSignupRequest("잠금테스트", "1234", "1234"));
        UserLoginRequest wrongPin = new UserLoginRequest("잠금테스트", "0000");

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> userService.login(wrongPin))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_LOGIN_FAILED);
        }

        UserLoginRequest correctPin = new UserLoginRequest("잠금테스트", "1234");
        assertThatThrownBy(() -> userService.login(correctPin))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_LOGIN_LOCKED);
    }
}
