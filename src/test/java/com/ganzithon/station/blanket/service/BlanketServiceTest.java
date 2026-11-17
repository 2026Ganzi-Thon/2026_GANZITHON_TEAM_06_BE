package com.ganzithon.station.blanket.service;

import com.ganzithon.station.blanket.dto.request.BlanketPieceCreateRequest;
import com.ganzithon.station.blanket.dto.response.PublicCardResponse;
import com.ganzithon.station.blanket.dto.response.PublicCardStatus;
import com.ganzithon.station.blanket.entity.BlanketPiece;
import com.ganzithon.station.blanket.repository.BlanketPieceRepository;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.room.entity.Pattern;
import com.ganzithon.station.ticket.entity.EndReason;
import com.ganzithon.station.user.entity.User;
import com.ganzithon.station.user.service.UserService;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
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

    @Test
    void 비공개면_조각이_있어도_PRIVATE이고_조각을_조회하지_않음() {
        User user = mock(User.class);
        when(user.isPublic()).thenReturn(false);
        when(userService.getUserEntity(1L)).thenReturn(user);

        PublicCardResponse response = blanketService.getPublicCard(1L);

        assertThat(response.status()).isEqualTo(PublicCardStatus.PRIVATE);
        assertThat(response.card()).isNull();
        verifyNoInteractions(blanketPieceRepository);
    }

    @Test
    void 공개인데_조각이_없으면_NO_PIECE() {
        User user = mock(User.class);
        when(user.isPublic()).thenReturn(true);
        when(userService.getUserEntity(1L)).thenReturn(user);
        when(blanketPieceRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                1L, PageRequest.of(0, BlanketService.MAX_VISIBLE_PIECE_COUNT)))
                .thenReturn(List.of());

        PublicCardResponse response = blanketService.getPublicCard(1L);

        assertThat(response.status()).isEqualTo(PublicCardStatus.NO_PIECE);
        assertThat(response.card()).isNull();
    }

    @Test
    void 공개이고_조각이_있으면_PUBLIC이고_card에_조회된_순서대로_도안_코드가_담김() {
        User user = mock(User.class);
        when(user.isPublic()).thenReturn(true);
        when(userService.getUserEntity(1L)).thenReturn(user);

        BlanketPiece latestPiece = mock(BlanketPiece.class);
        Pattern latestPattern = mock(Pattern.class);
        when(latestPattern.getCode()).thenReturn("PATTERN_02");
        when(latestPiece.getPattern()).thenReturn(latestPattern);

        BlanketPiece olderPiece = mock(BlanketPiece.class);
        Pattern olderPattern = mock(Pattern.class);
        when(olderPattern.getCode()).thenReturn("PATTERN_01");
        when(olderPiece.getPattern()).thenReturn(olderPattern);

        when(blanketPieceRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                1L, PageRequest.of(0, BlanketService.MAX_VISIBLE_PIECE_COUNT)))
                .thenReturn(List.of(latestPiece, olderPiece));

        PublicCardResponse response = blanketService.getPublicCard(1L);

        assertThat(response.status()).isEqualTo(PublicCardStatus.PUBLIC);
        assertThat(response.card()).containsExactly("PATTERN_02", "PATTERN_01");
    }
}
