package com.ganzithon.station.global;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.room.entity.MoodChip;
import com.ganzithon.station.room.entity.Pattern;
import com.ganzithon.station.room.entity.Room;
import com.ganzithon.station.ticket.entity.Ticket;
import com.ganzithon.station.ticket.entity.TicketStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flyway 스키마(V1)와 시드(V2) 확인. 엔티티와 스키마가 다르면 ddl-auto=validate로 컨텍스트가 뜨지 않습니다.
 * <p>다른 테스트와 DB 컨테이너를 같이 쓰므로(실행 순서는 정해져 있지 않음) 승차권이 없는 빈 DB를 가정하지 않습니다.
 * 직접 넣는 승차권 번호는 발급 카운터가 닿지 않는 큰 값을 씁니다.</p>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class SchemaSeedTest {

    /** 발급 API가 매기는 번호(1부터)와 겹치지 않는 번호 */
    private static final int FAR_TICKET_NUMBER = 1_000_000;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Test
    void 대기실_3개가_순서대로_있고_방마다_번호_카운터가_있다() {
        List<String> codes = jdbcTemplate.queryForList("select code from rooms order by sort_order", String.class);
        assertThat(codes).containsExactly("RESULT", "NO_REPLY", "GANZITHON_FINAL");

        Integer counterSum = jdbcTemplate.queryForObject(
                "select count(*) from room_ticket_counters c join rooms r on r.id = c.room_id where c.last_number >= 0",
                Integer.class);
        assertThat(counterSum).isEqualTo(3);
    }

    @Test
    void 상황_칩은_방별_개수와_순서가_맞음() {
        assertThat(chipCodes("RESULT")).hasSize(7).first().isEqualTo("RESULT_AFTER_INTERVIEW");
        assertThat(chipCodes("NO_REPLY")).hasSize(6).last().isEqualTo("NO_REPLY_ONE_LINE");
        assertThat(chipCodes("GANZITHON_FINAL")).hasSize(5).last().isEqualTo("FINAL_LAST_COMMIT");
    }

    @Test
    void 도안_임시_3개() {
        List<String> codes = jdbcTemplate.queryForList("select code from patterns order by code", String.class);
        assertThat(codes).containsExactly("PATTERN_01", "PATTERN_02", "PATTERN_03");
    }

    @Test
    void 게스트_승차권을_엔티티로_저장하고_읽을_수_있음() {
        Room room = findRoom("RESULT");
        Pattern pattern = entityManager.createQuery("select p from Pattern p where p.code = 'PATTERN_01'", Pattern.class)
                .getSingleResult();

        Ticket ticket = Ticket.builder()
                .room(room)
                .ticketNumber(FAR_TICKET_NUMBER)
                .guestToken("guest-token-for-test")
                .moodChip(MoodChip.FLUTTERING)
                .pattern(pattern)
                .startedAt(Instant.now())
                .build();
        entityManager.persist(ticket);
        entityManager.flush();
        entityManager.clear();

        Ticket found = entityManager.find(Ticket.class, ticket.getId());
        assertThat(found.getStatus()).isEqualTo(TicketStatus.WAITING);
        assertThat(found.getMoodChip()).isEqualTo(MoodChip.FLUTTERING);
        assertThat(found.getYarnSpent()).isZero();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void check_소유자가_없으면_저장_불가() {
        assertThatThrownBy(() -> insertTicket(1, "null", "'WAITING'", "null", "null", 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_tickets_owner");
    }

    @Test
    void check_ENDED인데_종료_사유가_없으면_저장_불가() {
        assertThatThrownBy(() -> insertTicket(2, "'guest'", "'ENDED'", "null", "now()", 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_tickets_ended");
    }

    @Test
    void check_쓴_털실이_음수면_저장_불가() {
        assertThatThrownBy(() -> insertTicket(3, "'guest'", "'WAITING'", "null", "null", -1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_tickets_yarn_spent");
    }

    @Test
    void unique_같은_방에_같은_승차권_번호는_불가() {
        insertTicket(4, "'guest'", "'WAITING'", "null", "null", 0);
        assertThatThrownBy(() -> insertTicket(4, "'guest'", "'WAITING'", "null", "null", 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_tickets_room_ticket_number");
    }

    private List<String> chipCodes(String roomCode) {
        return jdbcTemplate.queryForList("""
                select c.code from situation_chips c join rooms r on r.id = c.room_id
                where r.code = ? order by c.sort_order
                """, String.class, roomCode);
    }

    private Room findRoom(String code) {
        return entityManager.createQuery("select r from Room r where r.code = :code", Room.class)
                .setParameter("code", code)
                .getSingleResult();
    }

    private void insertTicket(int ticketNumber, String guestToken, String status, String endReason,
                              String endedAt, int yarnSpent) {
        jdbcTemplate.update("""
                insert into tickets (room_id, ticket_number, guest_token, pattern_id, status, end_reason,
                                     started_at, ended_at, yarn_spent)
                values ((select id from rooms where code = 'RESULT'), %d + %d, %s,
                        (select id from patterns where code = 'PATTERN_01'), %s, %s, now(), %s, %d)
                """.formatted(FAR_TICKET_NUMBER, ticketNumber, guestToken, status, endReason, endedAt, yarnSpent));
    }
}
