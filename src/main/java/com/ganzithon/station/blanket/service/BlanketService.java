package com.ganzithon.station.blanket.service;

import com.ganzithon.station.blanket.dto.request.BlanketPieceCreateRequest;
import com.ganzithon.station.blanket.dto.response.BlanketPieceResponse;
import com.ganzithon.station.blanket.dto.response.PublicCardResponse;
import com.ganzithon.station.blanket.dto.response.PublicCardStatus;
import com.ganzithon.station.blanket.entity.BlanketPiece;
import com.ganzithon.station.blanket.repository.BlanketPieceRepository;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.room.entity.Pattern;
import com.ganzithon.station.ticket.entity.Ticket;
import com.ganzithon.station.user.entity.User;
import com.ganzithon.station.user.service.UserService;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

    /** 담요 격자(3×3)에 맞춰 카드·목록에 담는 조각 상한. 내 담요 목록 조회에서도 재사용합니다. */
    public static final int MAX_VISIBLE_PIECE_COUNT = 9;

    private static final String GUEST_MESSAGE = "게스트 유저입니다.";
    private static final String PRIVATE_MESSAGE = "공개되어 있지 않습니다.";
    private static final String NO_PIECE_MESSAGE = "보유한 조각이 없습니다.";

    private final UserService userService;
    private final BlanketPieceRepository blanketPieceRepository;
    private final EntityManager entityManager;

    // ── 다른 도메인 Service용 (Controller에서 쓰지 않음) ──

    /**
     * 계정 담요 조각 1칸 저장.
     * <p>구현: 상진 / 호출: ticket(로그인 상태로 기다림 종료, 임시 조각 옮기기)</p>
     * <p>하는 일: 받은 값을 그대로 저장만 합니다. 열차 수·기다림 이름을 다시 계산하지 않습니다 (계산은 ticket 담당).</p>
     * <ul>
     *     <li>계정: {@code userService.getUserEntity(request.userId())}</li>
     *     <li>Ticket·Pattern: 아래 {@link #ticketReference}, {@link #patternReference}를 그대로 쓰면 됩니다</li>
     *     <li>검증: 같은 ticketId로 조각이 이미 있으면 BLANKET_PIECE_ALREADY_EXISTS (DB unique 제약도 있지만 먼저 확인)</li>
     *     <li>뒷면 기록은 저장 후 바꾸지 않습니다 (BlanketPiece에 수정 메서드 없음)</li>
     * </ul>
     * 호출하는 TicketService의 트랜잭션 안에서 실행되므로, 여기서 실패하면 기다림 종료도 함께 취소됩니다.
     *
     * @throws BusinessException USER_NOT_FOUND 없는 계정, BLANKET_PIECE_ALREADY_EXISTS 이미 조각이 있음
     */
    @Transactional
    public BlanketPieceResponse createPiece(BlanketPieceCreateRequest request) {
        if (blanketPieceRepository.existsByTicketId(request.ticketId())) {
            throw new BusinessException(ErrorCode.BLANKET_PIECE_ALREADY_EXISTS);
        }
        User user = userService.getUserEntity(request.userId());
        BlanketPiece blanketPiece = BlanketPiece.builder()
                .ticket(ticketReference(request.ticketId()))
                .user(user)
                .pattern(patternReference(request.patternId()))
                .ticketNumber(request.ticketNumber())
                .waitingName(request.waitingName())
                .trainCount(request.trainCount())
                .endReason(request.endReason())
                .build();
        return BlanketPieceResponse.from(blanketPieceRepository.save(blanketPiece));
    }

    /**
     * 조각에 연결할 Ticket. DB를 조회하지 않고 ID만 가진 참조를 돌려줍니다.
     * <p>Service가 아니라 ID로 연결만 하므로 호출 방향 위반 아님 (blanket → ticket 호출이 아닙니다).
     * 존재 여부는 호출한 TicketService가 이미 확인했고, 혹시 없으면 저장할 때 FK 제약으로 실패합니다.</p>
     */
    private Ticket ticketReference(Long ticketId) {
        return entityManager.getReference(Ticket.class, ticketId);
    }

    /**
     * 조각 앞면에 연결할 Pattern. DB를 조회하지 않고 ID만 가진 참조를 돌려줍니다.
     * <p>Service가 아니라 ID로 연결만 하므로 호출 방향 위반 아님 (blanket → room 호출이 아닙니다).</p>
     */
    private Pattern patternReference(Long patternId) {
        return entityManager.getReference(Pattern.class, patternId);
    }

    /**
     * 대기실 공개 카드. 승객을 탭했을 때 보여 줄 데이터.
     * <p>구현: 상진 / 호출: realtime (REST 없음)</p>
     * <p>판단 순서: 비공개 → PRIVATE, 조각 없음 → NO_PIECE, 그 외 → PUBLIC. 비공개면 조각을 조회하지 않습니다.</p>
     *
     * @throws BusinessException USER_NOT_FOUND 없는 계정
     */
    public PublicCardResponse getPublicCard(Long userId) {
        User user = userService.getUserEntity(userId);
        if (!user.isPublic()) {
            return new PublicCardResponse(PublicCardStatus.PRIVATE, PRIVATE_MESSAGE, null);
        }

        List<BlanketPiece> pieces = blanketPieceRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                userId, PageRequest.of(0, MAX_VISIBLE_PIECE_COUNT));
        if (pieces.isEmpty()) {
            return new PublicCardResponse(PublicCardStatus.NO_PIECE, NO_PIECE_MESSAGE, null);
        }

        List<String> patternCodes = pieces.stream()
                .map(piece -> piece.getPattern().getCode())
                .toList();
        return new PublicCardResponse(PublicCardStatus.PUBLIC, null, patternCodes);
    }

    /**
     * 비로그인 승객용 대기실 공개 카드. realtime이 userId가 없을 때 호출합니다.
     */
    public PublicCardResponse getGuestPublicCard() {
        return new PublicCardResponse(PublicCardStatus.GUEST, GUEST_MESSAGE, null);
    }
}
