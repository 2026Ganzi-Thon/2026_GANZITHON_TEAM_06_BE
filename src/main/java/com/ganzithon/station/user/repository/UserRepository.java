package com.ganzithon.station.user.repository;

import com.ganzithon.station.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNickname(String nickname);

    /** 로그인에서만 사용. 같은 계정의 로그인 실패 횟수 갱신이 겹치지 않도록 비관적 락으로 조회합니다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.nickname = :nickname")
    Optional<User> findWithLockByNickname(String nickname);

    boolean existsByNickname(String nickname);
}
