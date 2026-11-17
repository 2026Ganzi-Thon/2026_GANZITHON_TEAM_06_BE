package com.ganzithon.station.user.service;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
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
 * 같은 닉네임으로 동시에 가입 요청이 오면, existsByNickname~save 사이에 겹쳐서
 * 유니크 제약 위반(DataIntegrityViolationException)이 처리되지 않고 500이 될 수 있습니다(재현용).
 * 수정 후에는 성공 1개 + USER_NICKNAME_DUPLICATED 19개가 나와야 합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserServiceSignupRaceIntegrationTest {

    private static final int REQUEST_COUNT = 20;

    @Autowired
    UserService userService;

    @Test
    void 같은_닉네임으로_동시에_20번_가입하면_1개만_성공하고_나머지는_중복이다() throws InterruptedException {
        String nickname = "동시가입테스트";

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger duplicatedCount = new AtomicInteger();
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
                    userService.signup(new UserSignupRequest(nickname, "1234", "1234"));
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.USER_NICKNAME_DUPLICATED) {
                        duplicatedCount.incrementAndGet();
                    } else {
                        otherCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    otherCount.incrementAndGet();
                    System.out.println("OTHER EXCEPTION: " + e.getClass().getName() + " - " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("finished=" + finished
                + " SUCCESS=" + successCount.get()
                + " DUPLICATED=" + duplicatedCount.get()
                + " OTHER=" + otherCount.get());

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(duplicatedCount.get()).isEqualTo(19);
        assertThat(otherCount.get()).isZero();
    }
}
