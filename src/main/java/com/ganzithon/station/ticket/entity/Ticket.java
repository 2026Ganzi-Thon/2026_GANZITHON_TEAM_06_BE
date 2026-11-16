package com.ganzithon.station.ticket.entity;

import com.ganzithon.station.global.entity.BaseTimeEntity;
import com.ganzithon.station.room.entity.MoodChip;
import com.ganzithon.station.room.entity.Pattern;
import com.ganzithon.station.room.entity.Room;
import com.ganzithon.station.room.entity.SituationChip;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 승차권 = 기다림 한 건. 로그인 발급이면 user, 비로그인 발급이면 guestToken으로 소유자를 구분합니다.
 * 지나간 열차 수와 받은 털실 수는 저장하지 않고 startedAt으로 계산합니다.
 * 남은 털실 = 지나간 열차 수 - yarnSpent
 */
@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private int ticketNumber;

    /** 로그인 상태에서 발급했거나 계정으로 옮긴 경우 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** 비로그인 발급 시 서버가 만든 랜덤 값 */
    @Column(length = 64)
    private String guestToken;

    /** 건너뛰면 null. 방송 때 바뀔 수 있음 */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MoodChip moodChip;

    /** 마지막 방송의 상황 칩 (조각의 "기다림 이름"용) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_situation_chip_id")
    private SituationChip lastSituationChip;

    /** 발급 때 랜덤 배정 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pattern_id", nullable = false)
    private Pattern pattern;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EndReason endReason;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant endedAt;

    /** 털실 톡톡 "역장에게 전달" 시각 */
    private Instant knittingDeliveredAt;

    /** 임시 조각을 계정으로 옮긴 시각. 채워지면 다시 옮길 수 없음 */
    private Instant claimedAt;

    /** 쓴 털실 수 (도안 바꾸기 · 털실 1올) */
    @Column(nullable = false)
    private int yarnSpent;

    @Builder
    private Ticket(Room room, int ticketNumber, User user, String guestToken,
                   MoodChip moodChip, Pattern pattern, Instant startedAt) {
        this.room = room;
        this.ticketNumber = ticketNumber;
        this.user = user;
        this.guestToken = guestToken;
        this.moodChip = moodChip;
        this.pattern = pattern;
        this.startedAt = startedAt;
        this.status = TicketStatus.WAITING;
        this.yarnSpent = 0;
    }

    /**
     * 요청한 사람의 승차권인지. 계정(userId)이나 게스트 토큰 중 하나가 맞으면 소유자입니다.
     * (계정으로 옮긴 승차권은 둘 다 있어서 어느 쪽으로도 확인됩니다)
     */
    public boolean isOwnedBy(Long userId, String guestToken) {
        boolean ownedByUser = user != null && userId != null && userId.equals(user.getId());
        boolean ownedByGuest = this.guestToken != null && this.guestToken.equals(guestToken);
        return ownedByUser || ownedByGuest;
    }

    public boolean isWaiting() {
        return status == TicketStatus.WAITING;
    }

    /**
     * 대기실 호칭. 상태 칩이 있으면 "104번 두근두근한 양", 없으면 "104번 양".
     * 저장하지 않고 승차권 번호와 지금 상태 칩으로 만듭니다 (방송으로 상태 칩이 바뀌면 호칭도 바뀜).
     */
    public String getDisplayName() {
        if (moodChip == null) {
            return ticketNumber + "번 양";
        }
        return ticketNumber + "번 " + moodChip.getLabel() + " 양";
    }
}
