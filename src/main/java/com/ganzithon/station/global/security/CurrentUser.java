package com.ganzithon.station.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러에서 요청한 사람을 꺼냅니다. 파라미터 타입은 {@link Requester}입니다.
 * <pre>{@code
 * @PostMapping("/{ticketId}/end")
 * public ResponseEntity<ApiResponse<TicketEndResponse>> endTicket(
 *         @PathVariable Long ticketId,
 *         @CurrentUser Requester requester,
 *         @Valid @RequestBody TicketEndRequest request) {
 *     return ResponseEntity.ok(ApiResponse.success(ticketService.endTicket(ticketId, requester, request)));
 * }
 * }</pre>
 * Swagger 문서에는 나오지 않습니다 (SwaggerConfig에서 숨김).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
