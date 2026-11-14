package com.ganzithon.station.room.service;

import com.ganzithon.station.global.exception.BusinessException;
import com.ganzithon.station.global.exception.ErrorCode;
import com.ganzithon.station.room.dto.response.RoomListResponse;
import com.ganzithon.station.room.dto.response.RoomResponse;
import com.ganzithon.station.room.entity.Pattern;
import com.ganzithon.station.room.entity.Room;
import com.ganzithon.station.room.entity.SituationChip;
import com.ganzithon.station.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 대기실 정보 (담당: 하빈). 방, 상황 칩, 도안은 시드 데이터라 조회만 합니다.
 * 호출하는 쪽: ticket, realtime. room은 다른 도메인 Service를 호출하지 않습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;

    // ── API용 (RoomController) ──

    public RoomListResponse getRoomList() {
        return RoomListResponse.from(getRoomEntityList());
    }

    public RoomResponse getRoom(String roomCode) {
        return RoomResponse.from(getRoomEntity(roomCode));
    }

    // ── 다른 도메인 Service용 (Controller에서 쓰지 않음) ──

    /**
     * 대기실 하나. 호출: ticket(승차권 발급), realtime(입장).
     *
     * @throws BusinessException ROOM_NOT_FOUND 없는 대기실 코드
     */
    public Room getRoomEntity(String roomCode) {
        return roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    /**
     * 모든 대기실 (sort_order 순). 호출: realtime(서버 시작 시 방마다 가상 승객 만들기).
     */
    public List<Room> getRoomEntityList() {
        return roomRepository.findAllByOrderBySortOrderAsc();
    }

    /**
     * 그 대기실의 상황 칩 하나.
     * <p>구현: 하빈 / 호출: realtime(방송 요청 검증), ticket(마지막 방송 칩 기록)</p>
     * <p>검증: 칩 코드가 있어도 <b>다른 방의 칩이면</b> 안 됩니다 (결과발표 방에서 읽씹 칩으로 방송 금지).</p>
     *
     * @throws BusinessException ROOM_NOT_FOUND 없는 대기실, SITUATION_CHIP_NOT_FOUND 없거나 다른 방의 칩
     */
    public SituationChip getSituationChipEntity(String roomCode, String situationChipCode) {
        // TODO(하빈): SituationChipRepository로 room.code와 chip.code를 같이 조건으로 조회
        throw new UnsupportedOperationException("TODO(하빈): getSituationChipEntity");
    }

    /**
     * 그 대기실의 상황 칩 전체 (sort_order 순).
     * <p>구현: 하빈 / 호출: realtime(가상 승객이 랜덤으로 칩을 고름)</p>
     *
     * @throws BusinessException ROOM_NOT_FOUND 없는 대기실
     */
    public List<SituationChip> getSituationChipEntityList(String roomCode) {
        // TODO(하빈): 상황 칩 목록 API(ON-02/SF-02)를 만들 때 같이 구현
        throw new UnsupportedOperationException("TODO(하빈): getSituationChipEntityList");
    }

    /**
     * 랜덤 도안 하나 (YT-01).
     * <p>구현: 하빈 / 호출: ticket(승차권 발급 때 도안 배정)</p>
     * <p>주의: 도안은 시드라 요청마다 DB를 읽지 말고, 서버 시작 시 메모리에 올려 두고 고릅니다 (DOMAIN.md 7장).</p>
     */
    public Pattern getRandomPattern() {
        // TODO(하빈)
        throw new UnsupportedOperationException("TODO(하빈): getRandomPattern");
    }

    /**
     * 지금 도안을 뺀 나머지 중 랜덤 하나.
     * <p>구현: 하빈 / 호출: ticket(털실 톡톡 "도안 바꾸기")</p>
     * <p>검증: 반환값은 반드시 {@code currentPatternId}와 달라야 합니다. 도안이 1개뿐이라 고를 게 없을 때의 처리는 환희와 상의.</p>
     */
    public Pattern getRandomPatternExcept(Long currentPatternId) {
        // TODO(하빈)
        throw new UnsupportedOperationException("TODO(하빈): getRandomPatternExcept");
    }
}
