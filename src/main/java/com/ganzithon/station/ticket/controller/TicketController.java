package com.ganzithon.station.ticket.controller;

import com.ganzithon.station.global.response.ApiResponse;
import com.ganzithon.station.global.security.CurrentUser;
import com.ganzithon.station.global.security.Requester;
import com.ganzithon.station.ticket.dto.request.TicketCreateRequest;
import com.ganzithon.station.ticket.dto.request.TicketEndRequest;
import com.ganzithon.station.ticket.dto.response.TicketCreateResponse;
import com.ganzithon.station.ticket.dto.response.TicketEndResponse;
import com.ganzithon.station.ticket.dto.response.TicketStatsResponse;
import com.ganzithon.station.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ticket", description = "승차권·기다림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "승차권 발급",
            description = """
                    대기실에 들어갈 승차권을 발급합니다 (ON-03, ON-04). 토큰 없이 호출할 수 있습니다.
                    - 계정 토큰이 있으면 그 계정의 승차권이 되고, guestToken은 null입니다.
                    - 비로그인이면 guestToken을 내려줍니다. 브라우저에 보관했다가 이후 요청의 X-Guest-Token 헤더로 보내세요.
                      이전에 받은 guestToken을 X-Guest-Token으로 보내면 같은 토큰을 다시 씁니다.
                    - 승차권 번호는 대기실마다 1번부터 매기고, 도안은 랜덤으로 정합니다.
                    - 없는 대기실이면 404 ROOM_NOT_FOUND, roomCode가 없거나 moodChip이 목록에 없으면 400 COMMON_INVALID_INPUT""")
    @PostMapping
    public ResponseEntity<ApiResponse<TicketCreateResponse>> createTicket(
            @CurrentUser Requester requester,
            @Valid @RequestBody TicketCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ticketService.createTicket(requester, request)));
    }

    @Operation(summary = "기다림 종료",
            description = """
                    기다림을 끝내고 담요 조각 1칸을 받습니다 (EN-01, EN-02). 내 승차권이어야 합니다.
                    - 로그인 발급분은 조각이 계정에 저장됩니다 (savedToAccount = true).
                    - 비로그인 발급분은 저장하지 않습니다 (savedToAccount = false). 응답을 브라우저에 임시 보관했다가,
                      가입·로그인 후 POST /api/v1/tickets/claim에 ticketId를 보내 계정으로 옮기세요.
                    - 종료하면 대기실에서 자동으로 나가집니다 (방에 LEAVE 메시지가 갑니다).
                    - 회고 질문 답변은 저장하지 않아서 보내지 않습니다.
                    - 남의 승차권이면 403 TICKET_FORBIDDEN, 이미 끝났으면 409 TICKET_ALREADY_ENDED,
                      endReason이 없거나 목록에 없으면 400 COMMON_INVALID_INPUT""")
    @PostMapping("/{ticketId}/end")
    public ResponseEntity<ApiResponse<TicketEndResponse>> endTicket(
            @PathVariable Long ticketId,
            @CurrentUser Requester requester,
            @Valid @RequestBody TicketEndRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.endTicket(ticketId, requester, request)));
    }

    @Operation(summary = "전광판 통계",
            description = "끝난 기다림 수를 종료 사유별로 내려줍니다 (BR-04). 모든 대기실 합산이고, 토큰 없이 호출할 수 있습니다.")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TicketStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getStats()));
    }
}
