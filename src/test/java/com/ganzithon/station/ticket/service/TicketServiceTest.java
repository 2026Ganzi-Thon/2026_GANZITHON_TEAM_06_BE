package com.ganzithon.station.ticket.service;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.global.security.Requester;
import com.ganzithon.station.ticket.dto.request.TicketCreateRequest;
import com.ganzithon.station.ticket.dto.request.TicketEndRequest;
import com.ganzithon.station.ticket.dto.response.TicketCreateResponse;
import com.ganzithon.station.ticket.entity.EndReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 요청. 같은 대기실에 여러 명이 동시에 발급받아도 번호가 겹치거나 빠지지 않고,
 * 같은 승차권을 동시에 종료해도 한 번만 성공해야 합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TicketServiceTest {

    private static final int CONCURRENT_REQUESTS = 20;

    @Autowired
    TicketService ticketService;

    @Test
    void 동시에_발급해도_번호가_겹치거나_빠지지_않는다() throws Exception {
        Requester guest = new Requester(null, null);
        TicketCreateRequest request = new TicketCreateRequest("RESULT", null);
        List<Callable<TicketCreateResponse>> tasks = IntStream.range(0, CONCURRENT_REQUESTS)
                .<Callable<TicketCreateResponse>>mapToObj(i -> () -> ticketService.createTicket(guest, request))
                .toList();

        List<Integer> numbers;
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)) {
            List<Future<TicketCreateResponse>> futures = executor.invokeAll(tasks);
            numbers = futures.stream().map(future -> {
                try {
                    return future.get().ticketNumber();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).sorted().toList();
        }

        int first = numbers.getFirst();
        assertThat(numbers).containsExactlyElementsOf(
                IntStream.range(first, first + CONCURRENT_REQUESTS).boxed().toList());
    }

    @Test
    void 같은_승차권을_동시에_종료해도_한_번만_성공하고_통계도_한_번만_오른다() throws Exception {
        TicketCreateResponse ticket = ticketService.createTicket(
                new Requester(null, null), new TicketCreateRequest("RESULT", null));
        Requester owner = new Requester(null, ticket.guestToken());
        long answeredBefore = ticketService.getStats().answeredCount();

        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)) {
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return ticketService.endTicket(ticket.ticketId(), owner, new TicketEndRequest(EndReason.ANSWERED));
                }));
            }
            start.countDown();
        }

        int succeeded = 0;
        List<ErrorCode> failures = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get();
                succeeded++;
            } catch (ExecutionException e) {
                failures.add(((BusinessException) e.getCause()).getErrorCode());
            }
        }
        assertThat(succeeded).isEqualTo(1);
        assertThat(failures).hasSize(CONCURRENT_REQUESTS - 1).containsOnly(ErrorCode.TICKET_ALREADY_ENDED);
        assertThat(ticketService.getStats().answeredCount()).isEqualTo(answeredBefore + 1);
    }
}
