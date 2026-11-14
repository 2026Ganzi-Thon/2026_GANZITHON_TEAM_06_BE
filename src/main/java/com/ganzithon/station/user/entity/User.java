package com.ganzithon.station.user.entity;

import com.ganzithon.station.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 계정. 닉네임+PIN으로 가입한 사람만 저장합니다. 비로그인 사용자는 User가 아닙니다.
 * 닉네임은 본인만 보는 이름이라 대기실에 내보내지 않습니다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String nickname;

    /** PasswordEncoder로 해시한 PIN. 평문 저장 금지 */
    @Column(nullable = false, length = 100)
    private String pinHash;

    /** 담요 대기실 공개 여부 (MY-04). 기본 비공개 */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    /** 연속 실패 횟수. 로그인 성공 시 0 */
    @Column(nullable = false)
    private int pinFailCount;

    /** 이 시각까지 로그인 잠금 */
    private Instant lockedUntil;

    @Builder
    private User(String nickname, String pinHash) {
        this.nickname = nickname;
        this.pinHash = pinHash;
        this.isPublic = false;
        this.pinFailCount = 0;
    }
}
