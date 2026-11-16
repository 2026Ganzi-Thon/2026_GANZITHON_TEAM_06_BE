package com.ganzithon.station.ticket.service;

import com.ganzithon.station.blanket.service.BlanketService;
import com.ganzithon.station.global.config.TrainProperties;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.global.security.GuestTokenGenerator;
import com.ganzithon.station.global.security.Requester;
import com.ganzithon.station.room.entity.MoodChip;
import com.ganzithon.station.room.entity.Pattern;
import com.ganzithon.station.room.entity.Room;
import com.ganzithon.station.room.entity.SituationChip;
import com.ganzithon.station.room.service.RoomService;
import com.ganzithon.station.ticket.dto.request.TicketCreateRequest;
import com.ganzithon.station.ticket.dto.response.BroadcastRecordResponse;
import com.ganzithon.station.ticket.dto.response.PassengerTicketResponse;
import com.ganzithon.station.ticket.dto.response.TicketClaimResponse;
import com.ganzithon.station.ticket.dto.response.TicketCreateResponse;
import com.ganzithon.station.ticket.entity.Ticket;
import com.ganzithon.station.ticket.repository.RoomTicketCounterRepository;
import com.ganzithon.station.ticket.repository.TicketRepository;
import com.ganzithon.station.user.entity.User;
import com.ganzithon.station.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final TicketRepository ticketRepository;
    private final RoomTicketCounterRepository roomTicketCounterRepository;
    private final GuestTokenGenerator guestTokenGenerator;
    private final TrainProperties trainProperties;

    // ── API용 (TicketController) ──

    /**
     * 승차권 발급 (ON-03, ON-04). POST /api/v1/tickets
     * <ul>
     *     <li>로그인 상태: 계정 소유. 게스트 토큰은 만들지 않습니다 (함께 보내도 무시)</li>
     *     <li>비로그인: 보낸 X-Guest-Token으로 발급한 승차권이 이미 있으면 그 토큰을 다시 쓰고, 아니면 새로 만듭니다.
     *         한 브라우저의 승차권이 토큰 하나로 묶여서 나중에 계정으로 옮기기(MY-08)가 쉽습니다.
     *         처음 보는 토큰은 쓰지 않습니다 (클라이언트가 정한 추측하기 쉬운 값을 막기 위해)</li>
     *     <li>도안은 랜덤 배정 (YT-01). 진행 중인 승차권이 있어도 새로 발급합니다</li>
     * </ul>
     *
     * @throws BusinessException ROOM_NOT_FOUND 없는 대기실, USER_NOT_FOUND 없는 계정
     */
    @Transactional
    public TicketCreateResponse createTicket(Requester requester, TicketCreateRequest request) {
        Room room = roomService.getRoomEntity(request.roomCode());
        User user = requester.isLoggedIn() ? userService.getUserEntity(requester.userId()) : null;
        String guestToken = requester.isLoggedIn() ? null : resolveGuestToken(requester.guestToken());
        Pattern pattern = roomService.getRandomPattern();
        int ticketNumber = roomTicketCounterRepository.incrementAndGetLastNumber(room.getId());
        Instant now = Instant.now();

        Ticket ticket = ticketRepository.save(Ticket.builder()
                .room(room)
                .ticketNumber(ticketNumber)
                .user(user)
                .guestToken(guestToken)
                .moodChip(request.moodChip())
                .pattern(pattern)
                .startedAt(now)
                .build());
        return TicketCreateResponse.of(ticket, now, trainProperties.interval());
    }

    private String resolveGuestToken(String requestedGuestToken) {
        if (requestedGuestToken != null && ticketRepository.existsByGuestToken(requestedGuestToken)) {
            return requestedGuestToken;
        }
        return guestTokenGenerator.generate();
    }

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
        Ticket ticket = getOwnedTicket(ticketId, requester);
        if (!ticket.isWaiting()) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_ENDED);
        }
        return new PassengerTicketResponse(
                ticket.getId(),
                ticket.getRoom().getCode(),
                ticket.getTicketNumber(),
                ticket.getDisplayName(),
                ticket.getMoodChip(),
                ticket.getStartedAt());
    }

    /** 요청한 사람의 승차권. 없으면 TICKET_NOT_FOUND, 남의 것이면 TICKET_FORBIDDEN */
    private Ticket getOwnedTicket(Long ticketId, Requester requester) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));
        if (!ticket.isOwnedBy(requester.userId(), requester.guestToken())) {
            throw new BusinessException(ErrorCode.TICKET_FORBIDDEN);
        }
        return ticket;
    }

    /**
     * 방송(BR-01)한 칩을 승차권에 기록하고 바뀐 호칭을 돌려줍니다.
     * <p>구현: 환희 / 호출: realtime(방송 요청이 쿨다운을 통과한 뒤)</p>
     * <p>하는 일: 상태 칩(필수)으로 호칭을 바꾸고, 마지막 상황 칩(조각의 "기다림 이름")을 기록합니다.
     * 칩이 그 방의 것인지는 {@code roomService.getSituationChipEntity}로 확인합니다.</p>
     *
     * <p>소유자는 대기실 입장 때 이미 확인했으므로 여기서 다시 확인하지 않습니다.</p>
     *
     * @return 바뀐 대기실 호칭과 상황 칩 문구 (방송 문장용)
     * @throws BusinessException TICKET_NOT_FOUND, TICKET_ALREADY_ENDED, SITUATION_CHIP_NOT_FOUND
     */
    @Transactional
    public BroadcastRecordResponse recordBroadcast(Long ticketId, String situationChipCode, MoodChip moodChip) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));
        if (!ticket.isWaiting()) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_ENDED);
        }
        SituationChip situationChip = roomService.getSituationChipEntity(ticket.getRoom().getCode(), situationChipCode);
        ticket.recordBroadcast(situationChip, moodChip);
        return new BroadcastRecordResponse(ticket.getDisplayName(), situationChip.getLabel());
    }
}
