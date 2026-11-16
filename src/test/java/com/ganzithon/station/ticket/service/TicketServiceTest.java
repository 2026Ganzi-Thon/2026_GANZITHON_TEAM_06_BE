package com.ganzithon.station.ticket.service;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.global.security.Requester;
import com.ganzithon.station.room.entity.Pattern;
import com.ganzithon.station.room.service.RoomService;
import com.ganzithon.station.ticket.dto.request.TicketCreateRequest;
import com.ganzithon.station.ticket.dto.response.TicketCreateResponse;
import com.ganzithon.station.user.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

/**
 * 승차권 번호 동시 발급. 같은 대기실에 여러 명이 동시에 발급받아도 번호가 겹치거나 빠지지 않아야 합니다.
 * 아직 구현 전인 RoomService.getRandomPattern(하빈)은 이 테스트에서만 대신합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TicketServiceTest {

    private static final int CONCURRENT_REQUESTS = 20;

    @Autowired
    TicketService ticketService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @MockitoSpyBean
    RoomService roomService;

    @MockitoSpyBean
    UserService userService;

    @BeforeEach
    void stubOtherDomainTodos() {
        Long patternId = jdbcTemplate.queryForObject("select id from patterns where code = 'PATTERN_01'", Long.class);
        doAnswer(invocation -> entityManager.getReference(Pattern.class, patternId))
                .when(roomService).getRandomPattern();
    }

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
}
