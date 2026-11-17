package com.ganzithon.station.ticket.controller;

import com.ganzithon.station.global.response.ApiResponse;
import com.ganzithon.station.global.security.CurrentUser;
import com.ganzithon.station.global.security.Requester;
import com.ganzithon.station.ticket.dto.request.TicketCreateRequest;
import com.ganzithon.station.ticket.dto.request.TicketEndRequest;
import com.ganzithon.station.ticket.dto.response.KnittingDeliverResponse;
import com.ganzithon.station.ticket.dto.response.KnittingPatternChangeResponse;
import com.ganzithon.station.ticket.dto.response.TicketCreateResponse;
import com.ganzithon.station.ticket.dto.response.TicketEndResponse;
import com.ganzithon.station.ticket.dto.response.TicketResponse;
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

    @Operation(summary = "내 승차권 상태",
            description = """
                    새로고침했을 때 화면을 다시 그리는 데 씁니다 (TR-02, TR-04). 내 승차권이어야 합니다.
                    - trainCount(지나간 열차 수 = 받은 털실 수), remainingYarnCount(남은 털실)는 지금 기준으로 계산합니다.
                      끝난 기다림이면 종료 시각 기준입니다.
                    - knittingDeliveredAt이 있으면 털실 톡톡을 역장에게 전달한 상태입니다 (털실 보따리 아이콘).
                    - 남의 승차권이면 403 TICKET_FORBIDDEN, 없으면 404 TICKET_NOT_FOUND""")
    @GetMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(
            @PathVariable Long ticketId,
            @CurrentUser Requester requester) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicket(ticketId, requester)));
    }

    @Operation(summary = "털실 톡톡 도안 바꾸기",
            description = """
                    털실 1올을 쓰고 지금과 다른 도안으로 랜덤 교체합니다 (YT-03). body 없음.
                    - 바꾸면 처음부터 다시 채웁니다. 이미 역장에게 전달했다면 전달 기록이 지워집니다.
                    - 칸을 채우는 데는 털실을 쓰지 않습니다. 탭·채우기는 프론트가 관리합니다.
                    - 남은 털실이 없으면 409 KNITTING_NOT_ENOUGH_YARN, 끝난 기다림이면 409 TICKET_ALREADY_ENDED""")
    @PostMapping("/{ticketId}/knitting/change-pattern")
    public ResponseEntity<ApiResponse<KnittingPatternChangeResponse>> changeKnittingPattern(
            @PathVariable Long ticketId,
            @CurrentUser Requester requester) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.changeKnittingPattern(ticketId, requester)));
    }

    @Operation(summary = "털실 톡톡 역장에게 전달",
            description = """
                    다 뜬 털실 톡톡을 역장에게 전달했다고 기록합니다 (YT-04). body 없음. 서버는 전달 시각만 기록합니다.
                    - 이미 전달했으면 처음 전달한 시각을 그대로 돌려줍니다.
                    - 털실을 한 올도 못 받았으면(열차가 아직 안 지나감) 409 KNITTING_NOT_ENOUGH_YARN,
                      끝난 기다림이면 409 TICKET_ALREADY_ENDED
                    - 전달하지 않아도 종료 때 조각에는 도안이 완성본으로 들어갑니다 (YT-05).""")
    @PostMapping("/{ticketId}/knitting/deliver")
    public ResponseEntity<ApiResponse<KnittingDeliverResponse>> deliverKnitting(
            @PathVariable Long ticketId,
            @CurrentUser Requester requester) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.deliverKnitting(ticketId, requester)));
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
