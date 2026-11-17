package com.ganzithon.station.user.service;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.user.entity.User;
import com.ganzithon.station.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

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
}
