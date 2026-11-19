package com.ganzithon.station.realtime.controller;

import com.ganzithon.station.global.response.ApiResponse;
import com.ganzithon.station.realtime.dto.response.RoomOccupancyListResponse;
import com.ganzithon.station.realtime.service.RoomOccupancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Room", description = "대기실 API (인증 없이 호출 가능)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomOccupancyController {

    private final RoomOccupancyService roomOccupancyService;

    @Operation(summary = "대기실별 현재 인원 조회",
            description = "방 선택 화면(ON-02)에 보여 줄 인원입니다. 입장 전에도 호출할 수 있습니다. "
                    + "대기실 목록과 같은 순서이고, 아무도 없는 방은 0명입니다. 가상 승객은 세지 않습니다. "
                    + "실시간으로 밀어 주지 않으니 화면에 있는 동안 몇 초(예: 5초)마다 다시 조회해 주세요.")
    @GetMapping("/occupancy")
    public ResponseEntity<ApiResponse<RoomOccupancyListResponse>> getOccupancyList() {
        return ResponseEntity.ok(ApiResponse.success(roomOccupancyService.getOccupancyList()));
    }
}
