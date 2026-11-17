package com.ganzithon.station.ticket.service;

import com.ganzithon.station.blanket.dto.request.BlanketPieceCreateRequest;
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
import com.ganzithon.station.ticket.dto.request.TicketEndRequest;
import com.ganzithon.station.ticket.dto.response.BroadcastRecordResponse;
import com.ganzithon.station.ticket.dto.response.KnittingDeliverResponse;
import com.ganzithon.station.ticket.dto.response.KnittingPatternChangeResponse;
import com.ganzithon.station.ticket.dto.response.PassengerTicketResponse;
import com.ganzithon.station.ticket.dto.response.TicketClaimResponse;
import com.ganzithon.station.ticket.dto.response.TicketCreateResponse;
import com.ganzithon.station.ticket.dto.response.TicketEndResponse;
import com.ganzithon.station.ticket.dto.response.TicketResponse;
import com.ganzithon.station.ticket.dto.response.TicketStatsResponse;
import com.ganzithon.station.ticket.entity.Ticket;
import com.ganzithon.station.ticket.event.TicketEndedEvent;
import com.ganzithon.station.ticket.repository.RoomTicketCounterRepository;
import com.ganzithon.station.ticket.repository.TicketRepository;
import com.ganzithon.station.user.entity.User;
import com.ganzithon.station.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final TicketStatsCounter ticketStatsCounter;
    private final ApplicationEventPublisher eventPublisher;

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
     * 내 승차권 상태 (TR-02, TR-04). GET /api/v1/tickets/{ticketId}
     * <p>열차·털실 수는 저장하지 않고 지금(끝났으면 종료 시각) 기준으로 계산합니다.</p>
     *
     * @throws BusinessException TICKET_NOT_FOUND, TICKET_FORBIDDEN
     */
    public TicketResponse getTicket(Long ticketId, Requester requester) {
        Ticket ticket = getOwnedTicket(ticketId, requester);
        return TicketResponse.of(ticket, Instant.now(), trainProperties.interval());
    }

    /**
     * 털실 톡톡 도안 바꾸기 (YT-03). POST /api/v1/tickets/{ticketId}/knitting/change-pattern
     * <ul>
     *     <li>털실 1올을 쓰고 지금과 다른 도안으로 랜덤 교체합니다. 이미 전달했다면 전달 기록을 지웁니다 (처음부터 다시 채움)</li>
     *     <li>"남은 털실이 있을 때만 차감"을 UPDATE 한 번으로 해서, 두 번 빠르게 눌러도 털실이 남은 것보다 많이 빠지지 않습니다</li>
     * </ul>
     *
     * @throws BusinessException TICKET_NOT_FOUND, TICKET_FORBIDDEN, TICKET_ALREADY_ENDED, KNITTING_NOT_ENOUGH_YARN
     */
    @Transactional
    public KnittingPatternChangeResponse changeKnittingPattern(Long ticketId, Requester requester) {
        Ticket ticket = getOwnedTicket(ticketId, requester);
        requireWaiting(ticket);
        Instant now = Instant.now();
        int passedTrains = ticket.countPassedTrains(now, trainProperties.interval());
        if (ticket.countRemainingYarn(now, trainProperties.interval()) < 1) {
            throw new BusinessException(ErrorCode.KNITTING_NOT_ENOUGH_YARN);
        }

        Pattern newPattern = roomService.getRandomPatternExcept(ticket.getPattern().getId());
        // 위 확인과 이 UPDATE 사이에 다른 요청이 털실을 썼거나 종료했으면 여기서 걸립니다
        int yarnSpent = ticketRepository.spendYarnAndChangePattern(ticketId, newPattern.getId(), passedTrains)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNITTING_NOT_ENOUGH_YARN));
        return new KnittingPatternChangeResponse(newPattern.getCode(), passedTrains - yarnSpent);
    }

    /**
     * 털실 톡톡 "역장에게 전달" (YT-04). POST /api/v1/tickets/{ticketId}/knitting/deliver (body 없음)
     * <p>전달 시각만 기록합니다 (새로고침해도 털실 보따리 아이콘이 남도록). 탭·채우기는 전부 프론트가 하고 서버는 받지 않습니다.
     * 털실을 한 올이라도 받았어야(열차가 한 대 이상 지나감) 전달할 수 있습니다. 이미 전달했으면 처음 시각을 그대로 돌려줍니다.</p>
     *
     * @throws BusinessException TICKET_NOT_FOUND, TICKET_FORBIDDEN, TICKET_ALREADY_ENDED, KNITTING_NOT_ENOUGH_YARN
     */
    @Transactional
    public KnittingDeliverResponse deliverKnitting(Long ticketId, Requester requester) {
        Ticket ticket = getOwnedTicket(ticketId, requester);
        requireWaiting(ticket);
        Instant now = Instant.now();
        if (ticket.countPassedTrains(now, trainProperties.interval()) < 1) {
            throw new BusinessException(ErrorCode.KNITTING_NOT_ENOUGH_YARN);
        }
        return new KnittingDeliverResponse(ticket.deliverKnitting(now));
    }

    /**
     * 기다림 종료 (EN-01, EN-02). POST /api/v1/tickets/{ticketId}/end
     * <ul>
     *     <li>"진행 중일 때만 종료"를 UPDATE 한 번으로 해서, 같은 승차권을 동시에 종료해도 한 번만 성공합니다.
     *         그래서 조각 저장({@code createPiece})까지 두 번 가지 않습니다</li>
     *     <li>로그인 발급분(계정 소유): 조각을 계정에 저장합니다 (SV-01). 저장이 실패하면 종료도 함께 취소됩니다</li>
     *     <li>비로그인 발급분: 저장하지 않고 조각 내용만 돌려줍니다 (SV-04). 나중에 계정으로 옮길 때는 서버의 Ticket 기록으로 다시 만듭니다</li>
     *     <li>커밋되면 {@link TicketEndedEvent}로 전광판 통계를 올리고 대기실에서 내보냅니다</li>
     * </ul>
     * 회고 질문 답변(EN-03)은 저장하지 않습니다.
     *
     * @throws BusinessException TICKET_NOT_FOUND, TICKET_FORBIDDEN, TICKET_ALREADY_ENDED
     */
    @Transactional
    public TicketEndResponse endTicket(Long ticketId, Requester requester, TicketEndRequest request) {
        Ticket ticket = getOwnedTicket(ticketId, requester);
        Instant endedAt = Instant.now();
        if (!ticket.isWaiting() || ticketRepository.endIfWaiting(ticketId, request.endReason(), endedAt) == 0) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_ENDED);
        }

        int trainCount = ticket.countPassedTrains(endedAt, trainProperties.interval());
        boolean savedToAccount = ticket.getUser() != null;
        if (savedToAccount) {
            blanketService.createPiece(new BlanketPieceCreateRequest(
                    ticket.getId(),
                    ticket.getUser().getId(),
                    ticket.getPattern().getId(),
                    ticket.getTicketNumber(),
                    ticket.getWaitingName(),
                    trainCount,
                    request.endReason()));
        }
        eventPublisher.publishEvent(new TicketEndedEvent(ticket.getId(), ticket.getRoom().getCode(), request.endReason()));

        return new TicketEndResponse(
                ticket.getId(),
                ticket.getPattern().getCode(),
                ticket.getTicketNumber(),
                ticket.getWaitingName(),
                trainCount,
                request.endReason(),
                ticket.getStartedAt(),
                endedAt,
                savedToAccount);
    }

    /**
     * 전광판 통계 (BR-04). GET /api/v1/tickets/stats. 모든 대기실 합산, 메모리 카운터라 DB를 조회하지 않습니다.
     */
    public TicketStatsResponse getStats() {
        return ticketStatsCounter.snapshot();
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

    private static void requireWaiting(Ticket ticket) {
        if (!ticket.isWaiting()) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_ENDED);
        }
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
