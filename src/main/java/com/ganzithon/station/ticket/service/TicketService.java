package com.ganzithon.station.ticket.service;

import com.ganzithon.station.blanket.service.BlanketService;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.security.Requester;
import com.ganzithon.station.room.entity.MoodChip;
import com.ganzithon.station.room.service.RoomService;
import com.ganzithon.station.ticket.dto.response.PassengerTicketResponse;
import com.ganzithon.station.ticket.dto.response.TicketClaimResponse;
import com.ganzithon.station.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 승차권 = 기다림 한 건 (담당: 환희). 중심 도메인.
 * 호출 방향: realtime → ticket → blanket, ticket → room, ticket → user.
 * 소유 확인: 로그인 발급분은 userId, 비로그인 발급분은 guestToken으로 비교합니다. 보안 설정은 토큰이 "있는지"만 보므로
 * 승차권을 다루는 모든 메서드에서 반드시 소유자를 확인합니다 (TICKET_FORBIDDEN).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final RoomService roomService;
    private final UserService userService;
    private final BlanketService blanketService;

    /**
     * 비로그인으로 끝낸 기다림들을 계정으로 옮기기 (MY-08). POST /api/v1/tickets/claim
     * <p>구현: 환희 / 호출: TicketController (가입·로그인 직후 프론트가 호출)</p>
     * <p>각 승차권마다 검증</p>
     * <ul>
     *     <li>있는 승차권인지 → TICKET_NOT_FOUND</li>
     *     <li>{@code guestToken}이 그 승차권의 guest_token과 같은지 → TICKET_FORBIDDEN</li>
     *     <li>종료됐는지 → TICKET_NOT_ENDED</li>
     *     <li>이미 옮겼는지(claimed_at) → TICKET_ALREADY_CLAIMED</li>
     * </ul>
     * <p>통과하면 user_id, claimed_at을 채우고, Ticket 기록으로 뒷면 값을 계산해 {@code blanketService.createPiece}를 호출합니다.
     * 브라우저가 보관한 조각 내용은 믿지 않습니다. 하나라도 실패하면 전부 취소합니다 (한 트랜잭션).</p>
     *
     * @param userId     계정 토큰의 사용자 ({@code requester.requireUserId()})
     * @param guestToken X-Guest-Token. 없으면 AUTH_LOGIN_REQUIRED가 아니라 COMMON_INVALID_INPUT
     * @param ticketIds  옮길 승차권 ID 목록
     */
    @Transactional
    public TicketClaimResponse claimGuestTickets(Long userId, String guestToken, List<Long> ticketIds) {
        // TODO(환희)
        throw new UnsupportedOperationException("TODO(환희): claimGuestTickets");
    }

    // ── realtime용 (Controller에서 쓰지 않음) ──

    /**
     * 대기실 입장 때 승차권 확인.
     * <p>구현: 환희 / 호출: realtime(STOMP join)</p>
     * <p>검증: 소유자(TICKET_FORBIDDEN), 진행 중(TICKET_ALREADY_ENDED). 닉네임은 내보내지 않습니다.</p>
     *
     * @throws BusinessException TICKET_NOT_FOUND, TICKET_FORBIDDEN, TICKET_ALREADY_ENDED
     */
    public PassengerTicketResponse getPassengerTicket(Long ticketId, Requester requester) {
        // TODO(환희)
        throw new UnsupportedOperationException("TODO(환희): getPassengerTicket");
    }

    /**
     * 방송(BR-01)한 칩을 승차권에 기록하고 바뀐 호칭을 돌려줍니다.
     * <p>구현: 환희 / 호출: realtime(방송 요청이 쿨다운을 통과한 뒤)</p>
     * <p>하는 일: 상태 칩(필수)으로 호칭을 바꾸고, 마지막 상황 칩(조각의 "기다림 이름")을 기록합니다.
     * 칩이 그 방의 것인지는 {@code roomService.getSituationChipEntity}로 확인합니다.</p>
     *
     * @return 바뀐 대기실 호칭. 예: 104번 두근두근한 양
     * @throws BusinessException TICKET_NOT_FOUND, TICKET_ALREADY_ENDED, SITUATION_CHIP_NOT_FOUND
     */
    @Transactional
    public String recordBroadcast(Long ticketId, String situationChipCode, MoodChip moodChip) {
        // TODO(환희)
        throw new UnsupportedOperationException("TODO(환희): recordBroadcast");
    }
}
