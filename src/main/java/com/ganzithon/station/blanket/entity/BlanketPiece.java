package com.ganzithon.station.blanket.entity;

import com.ganzithon.station.global.entity.BaseTimeEntity;
import com.ganzithon.station.room.entity.Pattern;
import com.ganzithon.station.ticket.entity.EndReason;
import com.ganzithon.station.ticket.entity.Ticket;
import com.ganzithon.station.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 계정 담요 조각. 기다림 1건 = 조각 1칸. 비로그인 조각은 저장하지 않습니다 (브라우저 보관).
 * 뒷면 기록(ticketNumber, waitingName, trainCount, endReason)은 만들 때 복사하고 이후 바꾸지 않습니다.
 * 그래서 수정 메서드를 만들지 않습니다.
 */
@Entity
@Table(name = "blanket_pieces")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlanketPiece extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 앞면 도안 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pattern_id", nullable = false)
    private Pattern pattern;

    @Column(nullable = false)
    private int ticketNumber;

    /** 마지막 상황 칩 문구. 방송을 안 했으면 방 이름 */
    @Column(nullable = false, length = 50)
    private String waitingName;

    @Column(nullable = false)
    private int trainCount;

    /** 종료 표식. 공개 카드에는 내보내지 않음 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EndReason endReason;

    @Builder
    private BlanketPiece(Ticket ticket, User user, Pattern pattern, int ticketNumber,
                         String waitingName, int trainCount, EndReason endReason) {
        this.ticket = ticket;
        this.user = user;
        this.pattern = pattern;
        this.ticketNumber = ticketNumber;
        this.waitingName = waitingName;
        this.trainCount = trainCount;
        this.endReason = endReason;
    }
}
