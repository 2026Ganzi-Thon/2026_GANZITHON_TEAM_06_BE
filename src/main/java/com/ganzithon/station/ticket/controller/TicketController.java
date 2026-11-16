package com.ganzithon.station.ticket.controller;

import com.ganzithon.station.global.response.ApiResponse;
import com.ganzithon.station.global.security.CurrentUser;
import com.ganzithon.station.global.security.Requester;
import com.ganzithon.station.ticket.dto.request.TicketCreateRequest;
import com.ganzithon.station.ticket.dto.response.TicketCreateResponse;
import com.ganzithon.station.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
