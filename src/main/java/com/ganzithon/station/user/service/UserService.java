package com.ganzithon.station.user.service;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 (담당: 상진). 가입(MY-01), 로그인(MY-09), 공개 설정(MY-04).
 * 호출하는 쪽: ticket, blanket. user는 다른 도메인 Service를 호출하지 않습니다.
 * 가입·로그인 성공 시 토큰은 global/security의 AccountTokenProvider.issue(userId)로 발급합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    // ── 다른 도메인 Service용 (Controller에서 쓰지 않음) ──

    /**
     * 계정 하나.
     * <p>구현: 상진 / 호출: ticket(로그인 상태 승차권 발급, 임시 조각 옮기기), blanket(조각 저장)</p>
     * <p>주의: 반환한 User의 nickname은 본인 화면(마이 양)에서만 씁니다. 대기실·공개 카드로 내보내지 않습니다.</p>
     *
     * @throws BusinessException USER_NOT_FOUND 없는 계정
     */
    public User getUserEntity(Long userId) {
        // TODO(상진): UserRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
        throw new UnsupportedOperationException("TODO(상진): getUserEntity");
    }
}
