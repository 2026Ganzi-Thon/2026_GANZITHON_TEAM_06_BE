package com.ganzithon.station.ticket.service;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.global.security.Requester;
import com.ganzithon.station.ticket.dto.request.TicketCreateRequest;
import com.ganzithon.station.ticket.dto.request.TicketEndRequest;
import com.ganzithon.station.ticket.dto.response.TicketCreateResponse;
import com.ganzithon.station.room.entity.MoodChip;
import com.ganzithon.station.room.entity.SituationChip;
import com.ganzithon.station.room.service.RoomService;
import com.ganzithon.station.ticket.entity.EndReason;
import com.ganzithon.station.ticket.entity.Ticket;
import com.ganzithon.station.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 요청. 같은 대기실에 여러 명이 동시에 발급받아도 번호가 겹치거나 빠지지 않고,
 * 같은 승차권을 동시에 종료해도 한 번만 성공하고, 도안 바꾸기는 남은 털실보다 많이 성공하지 않아야 합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TicketServiceTest {

    private static final int CONCURRENT_REQUESTS = 20;

    @Autowired
    TicketService ticketService;

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    RoomService roomService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TransactionTemplate transactionTemplate;

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

    @Test
    void 도안_바꾸기를_동시에_눌러도_남은_털실만큼만_성공한다() throws Exception {
        TicketCreateResponse ticket = ticketService.createTicket(
                new Requester(null, null), new TicketCreateRequest("RESULT", null));
        Requester owner = new Requester(null, ticket.guestToken());
        // 공용 열차 2대가 지난 뒤 (테스트 간격 15초). 다음 열차가 2초 안이면 지나간 뒤까지 기다림
        long now = System.currentTimeMillis();
        if (15_000 - Math.floorMod(now, 15_000L) < 2_000) {
            Thread.sleep(15_000 - Math.floorMod(now, 15_000L) + 50);
            now = System.currentTimeMillis();
        }
        jdbcTemplate.update("update tickets set started_at = ? where id = ?",
                java.sql.Timestamp.from(java.time.Instant.ofEpochMilli(Math.floorDiv(now, 15_000L) * 15_000 - 2 * 15_000 + 1)),
                ticket.ticketId());

        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)) {
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return ticketService.changeKnittingPattern(ticket.ticketId(), owner);
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
        assertThat(succeeded).isEqualTo(2);
        assertThat(failures).containsOnly(ErrorCode.KNITTING_NOT_ENOUGH_YARN);
        assertThat(jdbcTemplate.queryForObject("select yarn_spent from tickets where id = ?", Integer.class, ticket.ticketId()))
                .isEqualTo(2);
    }

    @Test
    void 방송_기록이_늦게_저장돼도_종료가_되돌아가지_않는다() {
        TicketCreateResponse ticket = ticketService.createTicket(
                new Requester(null, null), new TicketCreateRequest("RESULT", null));
        Requester owner = new Requester(null, ticket.guestToken());
        SituationChip chip = roomService.getSituationChipEntity("RESULT", "RESULT_EXAM");

        transactionTemplate.executeWithoutResult(status -> {
            Ticket loaded = ticketRepository.findById(ticket.ticketId()).orElseThrow();
            // 방송 쪽이 승차권을 읽은 뒤, 다른 요청이 종료하고 먼저 커밋
            try (ExecutorService other = Executors.newSingleThreadExecutor()) {
                other.submit(() -> ticketService.endTicket(ticket.ticketId(), owner, new TicketEndRequest(EndReason.LET_GO)))
                        .get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            loaded.recordBroadcast(chip, MoodChip.CALM);
        });

        assertThat(jdbcTemplate.queryForMap("select status, end_reason, mood_chip from tickets where id = ?", ticket.ticketId()))
                .containsEntry("status", "ENDED")
                .containsEntry("end_reason", "LET_GO")
                .containsEntry("mood_chip", "CALM");
    }

    @Test
    void 같은_승차권을_동시에_계정으로_옮겨도_조각은_하나만_생긴다() throws Exception {
        TicketCreateResponse ticket = ticketService.createTicket(
                new Requester(null, null), new TicketCreateRequest("RESULT", null));
        ticketService.endTicket(ticket.ticketId(), new Requester(null, ticket.guestToken()),
                new TicketEndRequest(EndReason.ANSWERED));
        Long userId = jdbcTemplate.queryForObject(
                "insert into users (nickname, pin_hash) values (?, 'not-used') returning id", Long.class,
                java.util.UUID.randomUUID().toString().substring(0, 12));

        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)) {
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return ticketService.claimGuestTickets(userId, ticket.guestToken(), List.of(ticket.ticketId()));
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
        assertThat(failures).hasSize(CONCURRENT_REQUESTS - 1).containsOnly(ErrorCode.TICKET_ALREADY_CLAIMED);
        assertThat(jdbcTemplate.queryForObject("select count(*) from blanket_pieces where ticket_id = ?",
                Integer.class, ticket.ticketId())).isEqualTo(1);
    }
}
