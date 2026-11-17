package com.ganzithon.station.user.service;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.global.security.AccountToken;
import com.ganzithon.station.global.security.AccountTokenProvider;
import com.ganzithon.station.user.dto.request.UserLoginRequest;
import com.ganzithon.station.user.dto.request.UserPublicSettingUpdateRequest;
import com.ganzithon.station.user.dto.response.UserMeResponse;
import com.ganzithon.station.user.entity.User;
import com.ganzithon.station.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AccountTokenProvider accountTokenProvider;

    @InjectMocks
    UserService userService;

    @Test
    void 있는_계정이면_그대로_반환() {
        User user = User.builder().nickname("몽실이").pinHash("hashed").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.getUserEntity(1L)).isSameAs(user);
    }

    @Test
    void 없는_계정이면_USER_NOT_FOUND() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserEntity(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void PIN을_5번_연속_틀리면_잠금() {
        User user = User.builder().nickname("몽실이").pinHash("hashed").build();
        when(userRepository.findByNickname("몽실이")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        UserLoginRequest request = new UserLoginRequest("몽실이", "0000");

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_LOGIN_FAILED);
        }
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_LOGIN_FAILED);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_LOGIN_LOCKED);
    }

    @Test
    void 잠금_중에는_맞는_PIN도_거부() {
        User user = User.builder().nickname("몽실이").pinHash("hashed").build();
        Instant now = Instant.now();
        for (int i = 0; i < 5; i++) {
            user.recordLoginFailure(now);
        }
        when(userRepository.findByNickname("몽실이")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(new UserLoginRequest("몽실이", "1234")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_LOGIN_LOCKED);
    }

    @Test
    void 로그인_성공하면_실패_횟수_0으로_초기화() {
        User user = User.builder().nickname("몽실이").pinHash("hashed").build();
        user.recordLoginFailure(Instant.now());
        when(userRepository.findByNickname("몽실이")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "hashed")).thenReturn(true);
        lenient().when(accountTokenProvider.issue(any())).thenReturn(new AccountToken("token", Instant.now()));

        userService.login(new UserLoginRequest("몽실이", "1234"));

        assertThat(user.getPinFailCount()).isEqualTo(0);
    }

    @Test
    void 공개_설정을_바꾸면_반영된_내_정보를_반환() {
        User user = User.builder().nickname("몽실이").pinHash("hashed").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserMeResponse response = userService.updatePublicSetting(1L, new UserPublicSettingUpdateRequest(true));

        assertThat(user.isPublic()).isTrue();
        assertThat(response).isEqualTo(new UserMeResponse("몽실이", true));
    }

    @Test
    void 공개_설정_변경_시_없는_계정이면_USER_NOT_FOUND() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updatePublicSetting(1L, new UserPublicSettingUpdateRequest(true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
