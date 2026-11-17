package com.ganzithon.station.user.service;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.global.security.AccountTokenProvider;
import com.ganzithon.station.user.dto.request.UserLoginRequest;
import com.ganzithon.station.user.dto.request.UserPublicSettingUpdateRequest;
import com.ganzithon.station.user.dto.request.UserSignupRequest;
import com.ganzithon.station.user.dto.response.UserAuthResponse;
import com.ganzithon.station.user.dto.response.UserMeResponse;
import com.ganzithon.station.user.entity.User;
import com.ganzithon.station.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 계정 (담당: 상진). 가입(MY-01), 로그인(MY-09), 공개 설정(MY-04).
 * 호출하는 쪽: ticket, blanket. user는 다른 도메인 Service를 호출하지 않습니다.
 * 가입·로그인 성공 시 토큰은 global/security의 AccountTokenProvider.issue(userId)로 발급합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountTokenProvider accountTokenProvider;

    /**
     * 가입. 닉네임 중복이면 USER_NICKNAME_DUPLICATED.
     *
     * @throws BusinessException USER_NICKNAME_DUPLICATED 이미 있는 닉네임
     */
    @Transactional
    public UserAuthResponse signup(UserSignupRequest request) {
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.USER_NICKNAME_DUPLICATED);
        }
        User user = userRepository.save(User.builder()
                .nickname(request.nickname())
                .pinHash(passwordEncoder.encode(request.pin()))
                .build());
        return UserAuthResponse.of(user.getNickname(), accountTokenProvider.issue(user.getId()).accessToken());
    }

    /**
     * 로그인. 닉네임이 없든 PIN이 틀리든 똑같이 USER_LOGIN_FAILED, 연속 5회 실패하면 5분간 USER_LOGIN_LOCKED.
     *
     * @throws BusinessException USER_LOGIN_FAILED 닉네임 없음 또는 PIN 불일치, USER_LOGIN_LOCKED 잠금 중
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public UserAuthResponse login(UserLoginRequest request) {
        User user = userRepository.findByNickname(request.nickname())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_LOGIN_FAILED));

        Instant now = Instant.now();
        if (user.isLocked(now)) {
            throw new BusinessException(ErrorCode.USER_LOGIN_LOCKED);
        }
        if (!passwordEncoder.matches(request.pin(), user.getPinHash())) {
            user.recordLoginFailure(now);
            throw new BusinessException(ErrorCode.USER_LOGIN_FAILED);
        }
        user.resetLoginFailure();
        return UserAuthResponse.of(user.getNickname(), accountTokenProvider.issue(user.getId()).accessToken());
    }

    /** 내 정보 (닉네임, 공개 여부). */
    public UserMeResponse getMe(Long userId) {
        User user = getUserEntity(userId);
        return UserMeResponse.of(user.getNickname(), user.isPublic());
    }

    /** 담요 대기실 공개 설정 변경. */
    @Transactional
    public UserMeResponse updatePublicSetting(Long userId, UserPublicSettingUpdateRequest request) {
        User user = getUserEntity(userId);
        user.updatePublic(request.isPublic());
        return UserMeResponse.of(user.getNickname(), user.isPublic());
    }

    // ── 다른 도메인 Service용 (Controller에서 쓰지 않음) ──

    /**
     * 계정 하나.
     * <p>구현: 상진 / 호출: ticket(로그인 상태 승차권 발급, 임시 조각 옮기기), blanket(조각 저장)</p>
     * <p>주의: 반환한 User의 nickname은 본인 화면(마이 양)에서만 씁니다. 대기실·공개 카드로 내보내지 않습니다.</p>
     *
     * @throws BusinessException USER_NOT_FOUND 없는 계정
     */
    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
