package com.ganzithon.station.room.service;

import com.ganzithon.station.TestcontainersConfiguration;
import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.room.entity.Pattern;
import com.ganzithon.station.room.entity.SituationChip;
import com.ganzithon.station.room.repository.PatternRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 다른 도메인(ticket, realtime)이 호출하는 RoomService 메서드. 시드 데이터가 들어간 실제 Postgres로 확인합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RoomServiceTest {

    @Autowired
    RoomService roomService;

    @Autowired
    PatternRepository patternRepository;

    @Test
    void 그_방의_상황_칩은_찾는다() {
        SituationChip chip = roomService.getSituationChipEntity("NO_REPLY", "NO_REPLY_UNREAD_ONE");

        assertThat(chip.getCode()).isEqualTo("NO_REPLY_UNREAD_ONE");
        assertThat(chip.getLabel()).isEqualTo("아직 1이 안 사라진 중");
    }

    @Test
    void 다른_방의_상황_칩이면_SITUATION_CHIP_NOT_FOUND() {
        // 결과발표 방에서 읽씹 칩으로 방송하는 경우
        assertThatThrownBy(() -> roomService.getSituationChipEntity("RESULT", "NO_REPLY_UNREAD_ONE"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SITUATION_CHIP_NOT_FOUND);
    }

    @Test
    void 없는_상황_칩이면_SITUATION_CHIP_NOT_FOUND() {
        assertThatThrownBy(() -> roomService.getSituationChipEntity("RESULT", "NOT_EXIST"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SITUATION_CHIP_NOT_FOUND);
    }

    @Test
    void 없는_대기실이면_ROOM_NOT_FOUND() {
        assertThatThrownBy(() -> roomService.getSituationChipEntity("NOT_EXIST", "NO_REPLY_UNREAD_ONE"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void 상황_칩_목록은_sort_order_순서() {
        assertThat(roomService.getSituationChipEntityList("GANZITHON_FINAL"))
                .extracting(SituationChip::getCode)
                .containsExactly("FINAL_AFTER_PRESENTATION", "FINAL_JUDGING", "FINAL_ALL_NIGHTER",
                        "FINAL_AFTER_DEMO", "FINAL_LAST_COMMIT");
    }

    @Test
    void 랜덤_도안은_시드_도안_중_하나() {
        for (int i = 0; i < 20; i++) {
            assertThat(roomService.getRandomPattern().getCode())
                    .isIn("PATTERN_01", "PATTERN_02", "PATTERN_03");
        }
    }

    @Test
    void 도안_바꾸기는_지금_도안이_절대_나오지_않는다() {
        for (Pattern current : patternRepository.findAll()) {
            for (int i = 0; i < 20; i++) {
                assertThat(roomService.getRandomPatternExcept(current.getId()).getId())
                        .isNotEqualTo(current.getId());
            }
        }
    }
}
