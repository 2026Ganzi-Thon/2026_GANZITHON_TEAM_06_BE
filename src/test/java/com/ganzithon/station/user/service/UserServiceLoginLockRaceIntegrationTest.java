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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 계정에 틀린 PIN으로 동시에 로그인 요청이 오면, 실패 횟수가 "읽기~증가~저장" 사이에
 * 겹쳐서 5회 잠금이 걸리지 않을 수 있습니다(재현용). 수정 후에는 FAILED 5개 + LOCKED 15개가 나와야 합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserServiceLoginLockRaceIntegrationTest {

    private static final int REQUEST_COUNT = 20;

    @Autowired
    UserService userService;

    @Test
    void 같은_계정에_틀린_PIN으로_동시에_20번_요청하면_5번만_FAILED이고_나머지는_LOCKED이다() throws InterruptedException {
        userService.signup(new UserSignupRequest("동시로그인테스트", "1234", "1234"));
        UserLoginRequest wrongPin = new UserLoginRequest("동시로그인테스트", "0000");

        AtomicInteger failedCount = new AtomicInteger();
        AtomicInteger lockedCount = new AtomicInteger();
        AtomicInteger otherCount = new AtomicInteger();

        CountDownLatch readyLatch = new CountDownLatch(REQUEST_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(REQUEST_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);

        for (int i = 0; i < REQUEST_COUNT; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    userService.login(wrongPin);
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.USER_LOGIN_FAILED) {
                        failedCount.incrementAndGet();
                    } else if (e.getErrorCode() == ErrorCode.USER_LOGIN_LOCKED) {
                        lockedCount.incrementAndGet();
                    } else {
                        otherCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("FAILED=" + failedCount.get() + " LOCKED=" + lockedCount.get() + " OTHER=" + otherCount.get());

        assertThat(failedCount.get()).isEqualTo(5);
        assertThat(lockedCount.get()).isEqualTo(15);
        assertThat(otherCount.get()).isZero();
    }
}
