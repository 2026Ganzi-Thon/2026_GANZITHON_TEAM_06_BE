package com.ganzithon.station.blanket.controller;

import com.ganzithon.station.blanket.dto.response.BlanketPieceListResponse;
import com.ganzithon.station.blanket.dto.response.BlanketPieceResponse;
import com.ganzithon.station.blanket.service.BlanketService;
import com.ganzithon.station.global.response.ApiResponse;
import com.ganzithon.station.global.security.CurrentUser;
import com.ganzithon.station.global.security.Requester;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "BlanketPiece", description = "담요 조각 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/blanket-pieces")
public class BlanketPieceController {

    private final BlanketService blanketService;

    @Operation(summary = "내 담요 조각 목록",
            description = """
                    내 담요 조각을 최신 순(만든 시각 내림차순)으로 최대 9개 내려줍니다. 페이징 없음.
                    - 담요 격자가 3×3(9칸)이라 조각이 9개를 넘으면 가장 최신 9개만 내려주고, 오래된 조각은 지우지 않습니다.
                    - 로그인하지 않았으면 401 AUTH_LOGIN_REQUIRED""")
    @GetMapping
    public ResponseEntity<ApiResponse<BlanketPieceListResponse>> getBlanketPieceList(
            @CurrentUser Requester requester) {
        return ResponseEntity.ok(ApiResponse.success(blanketService.getBlanketPieceList(requester.requireUserId())));
    }

    @Operation(summary = "내 담요 조각 상세",
            description = """
                    담요 조각 하나의 앞면·뒷면 기록을 모두 내려줍니다.
                    - 로그인하지 않았으면 401 AUTH_LOGIN_REQUIRED
                    - 없는 조각이면 404 BLANKET_PIECE_NOT_FOUND, 남의 조각이면 403 BLANKET_PIECE_FORBIDDEN""")
    @GetMapping("/{pieceId}")
    public ResponseEntity<ApiResponse<BlanketPieceResponse>> getBlanketPiece(
            @PathVariable Long pieceId,
            @CurrentUser Requester requester) {
        return ResponseEntity.ok(
                ApiResponse.success(blanketService.getBlanketPiece(pieceId, requester.requireUserId())));
    }
}
