package com.ganzithon.station.blanket.service;

import com.ganzithon.station.blanket.dto.request.BlanketPieceCreateRequest;
import com.ganzithon.station.blanket.dto.response.BlanketPieceResponse;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담요 (담당: 상진). 계정 담요 조각 저장, 내 담요(MY-02, MY-03), 공개 카드(MY-05).
 * 호출 방향: ticket → blanket → user. blanket은 ticket·room Service를 호출하지 않습니다 (순환 참조 금지).
 * 임시 조각 옮기기(MY-08)는 TicketService.claimGuestTickets가 검증하고 여기 createPiece를 부릅니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlanketService {

    private final UserService userService;

    // ── 다른 도메인 Service용 (Controller에서 쓰지 않음) ──

    /**
     * 계정 담요 조각 1칸 저장.
     * <p>구현: 상진 / 호출: ticket(로그인 상태로 기다림 종료, 임시 조각 옮기기)</p>
     * <p>하는 일: 받은 값을 그대로 저장만 합니다. 열차 수·기다림 이름을 다시 계산하지 않습니다 (계산은 ticket 담당).</p>
     * <ul>
     *     <li>계정: {@code userService.getUserEntity(request.userId())}</li>
     *     <li>Ticket·Pattern 연결: ticket·room Service를 부르면 호출 방향이 거꾸로 되므로
     *         {@code EntityManager.getReference(Ticket.class, request.ticketId())}처럼 ID로만 연결합니다</li>
     *     <li>검증: 같은 ticketId로 조각이 이미 있으면 BLANKET_PIECE_ALREADY_EXISTS (DB unique 제약도 있지만 먼저 확인)</li>
     *     <li>뒷면 기록은 저장 후 바꾸지 않습니다 (BlanketPiece에 수정 메서드 없음)</li>
     * </ul>
     * 호출하는 TicketService의 트랜잭션 안에서 실행되므로, 여기서 실패하면 기다림 종료도 함께 취소됩니다.
     *
     * @throws BusinessException USER_NOT_FOUND 없는 계정, BLANKET_PIECE_ALREADY_EXISTS 이미 조각이 있음
     */
    @Transactional
    public BlanketPieceResponse createPiece(BlanketPieceCreateRequest request) {
        // TODO(상진)
        throw new UnsupportedOperationException("TODO(상진): createPiece");
    }
}
