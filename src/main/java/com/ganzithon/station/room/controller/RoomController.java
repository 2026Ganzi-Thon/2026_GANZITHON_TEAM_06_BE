package com.ganzithon.station.room.controller;

import com.ganzithon.station.global.response.ApiResponse;
import com.ganzithon.station.room.dto.response.RoomListResponse;
import com.ganzithon.station.room.dto.response.RoomResponse;
import com.ganzithon.station.room.dto.response.SituationChipListResponse;
import com.ganzithon.station.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Room", description = "대기실 API (인증 없이 호출 가능)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "대기실 목록 조회", description = "대기실 선택 화면(ON-02)에 보여 줄 순서대로 내려줍니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<RoomListResponse>> getRoomList() {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoomList()));
    }

    @Operation(summary = "대기실 단건 조회", description = "없는 코드면 404 ROOM_NOT_FOUND")
    @GetMapping("/{roomCode}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(
            @Parameter(description = "대기실 코드", example = "RESULT") @PathVariable String roomCode) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoom(roomCode)));
    }

    @Operation(summary = "상황 칩 목록 조회",
            description = "방송 작성 패널(BR-01)에 보여 줄 그 대기실의 상황 칩을 순서대로 내려줍니다. 칩은 고정 목록이고 자유 입력은 없습니다(SF-02). 없는 코드면 404 ROOM_NOT_FOUND")
    @GetMapping("/{roomCode}/situation-chips")
    public ResponseEntity<ApiResponse<SituationChipListResponse>> getSituationChipList(
            @Parameter(description = "대기실 코드", example = "NO_REPLY") @PathVariable String roomCode) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getSituationChipList(roomCode)));
    }
}
