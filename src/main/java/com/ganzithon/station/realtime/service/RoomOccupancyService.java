package com.ganzithon.station.realtime.service;

import com.ganzithon.station.realtime.dto.response.RoomOccupancyListResponse;
import com.ganzithon.station.realtime.dto.response.RoomOccupancyResponse;
import com.ganzithon.station.realtime.store.RoomPassengerStore;
import com.ganzithon.station.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 방 선택 화면(ON-02)에 보여 줄 대기실별 현재 인원 (담당: 환희). 메모리만 사용합니다.
 * room은 realtime을 호출할 수 없어서(호출 방향: realtime → room) 이 API는 realtime에 둡니다.
 */
@Service
@RequiredArgsConstructor
public class RoomOccupancyService {

    private final RoomService roomService;
    private final RoomPassengerStore roomPassengerStore;

    /** 모든 대기실 (sort_order 순). 아무도 없는 방도 0명으로 넣습니다 */
    public RoomOccupancyListResponse getOccupancyList() {
        int seatCount = roomPassengerStore.getSeatCount();
        return new RoomOccupancyListResponse(roomService.getRoomEntityList().stream()
                .map(room -> RoomOccupancyResponse.of(room.getCode(),
                        roomPassengerStore.countByRoomCode(room.getCode()), seatCount))
                .toList());
    }
}
