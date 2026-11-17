package com.ganzithon.station.blanket.service;

import com.ganzithon.station.blanket.dto.request.BlanketPieceCreateRequest;
import com.ganzithon.station.blanket.repository.BlanketPieceRepository;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.ticket.entity.EndReason;
import com.ganzithon.station.user.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlanketServiceTest {

    @Mock
    UserService userService;

    @Mock
    BlanketPieceRepository blanketPieceRepository;

    @Mock
    EntityManager entityManager;

    @InjectMocks
    BlanketService blanketService;

    @Test
    void 같은_승차권으로_두_번_만들면_BLANKET_PIECE_ALREADY_EXISTS() {
        BlanketPieceCreateRequest request = new BlanketPieceCreateRequest(
                104L, 1L, 1L, 104, "면접 보고 기다리는 중", 12, EndReason.ANSWERED);
        when(blanketPieceRepository.existsByTicketId(104L)).thenReturn(true);

        assertThatThrownBy(() -> blanketService.createPiece(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BLANKET_PIECE_ALREADY_EXISTS);

        verifyNoInteractions(userService, entityManager);
    }
}
