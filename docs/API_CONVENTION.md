# API 규칙

> REST API와 WebSocket(STOMP) 메시지 모두 이 문서를 따릅니다.
> API 문서는 Swagger로 확인합니다: `{서버 주소}/swagger-ui/index.html`

## 1. URL

- 모든 REST API는 **`/api/v1`** 로 시작합니다
- 자원(명사)은 **복수형**, 여러 단어는 **kebab-case**
- path variable은 **camelCase + Id**: `{ticketId}`. 코드로 찾는 자원은 **camelCase + Code**: `{roomCode}` (대기실은 시드 코드 `RESULT` 등으로 찾습니다)
- 동사를 URL에 넣지 않습니다. 대신 HTTP 메서드로 표현합니다

| ✅ 좋은 예 | ❌ 나쁜 예 |
|---|---|
| `GET /api/v1/rooms` | `GET /api/v1/getRooms` |
| `POST /api/v1/tickets` | `POST /api/v1/ticket/create` |
| `GET /api/v1/tickets/{ticketId}` | `GET /api/v1/tickets?id=3` |
| `GET /api/v1/users/me/blanket-pieces` | `GET /api/v1/myPieces` |

- **상태 전환**처럼 CRUD로 표현하기 어려운 동작은 마지막에 동작 이름을 붙이고 `POST`를 씁니다
  - `POST /api/v1/tickets/{ticketId}/end` (기다림 종료)
  - `POST /api/v1/tickets/{ticketId}/knitting/deliver` (털실 톡톡, 역장에게 전달)
- 쿼리 파라미터는 camelCase: `?roomId=1&status=WAITING`

## 2. HTTP 메서드와 상태 코드

| 메서드 | 용도 | 성공 상태 코드 |
|---|---|---|
| `GET` | 조회 | `200 OK` |
| `POST` | 생성 | `201 Created` |
| `POST` | 상태 전환 (`/end` 등) | `200 OK` |
| `PATCH` | 일부 수정 | `200 OK` |
| `DELETE` | 삭제 | `204 No Content` |

| 실패 상태 코드 | 언제 |
|---|---|
| `400 Bad Request` | 요청 값이 잘못됨 (검증 실패, 형식 오류) |
| `401 Unauthorized` | 로그인(인증)이 필요함 |
| `403 Forbidden` | 남의 데이터에 접근 |
| `404 Not Found` | 데이터가 없음 |
| `409 Conflict` | 지금 상태에서 할 수 없는 요청 (이미 종료된 기다림 다시 종료 등) |
| `429 Too Many Requests` | 쿨다운 중 |
| `500 Internal Server Error` | 서버 버그 (직접 반환하지 않음) |

## 3. 응답 형식

모든 REST 응답은 `ApiResponse`로 감쌉니다. JSON 필드는 **camelCase**입니다.

### 성공

```json
{
  "success": true,
  "data": {
    "ticketId": 104,
    "ticketNumber": 104,
    "startedAt": "2026-11-11T21:30:00+09:00"
  },
  "error": null
}
```

- 돌려줄 데이터가 없으면 `"data": null`
- 목록은 `data` 안에 배열 필드로 담습니다: `"data": { "blanketPieces": [ ... ] }`. `data` 자체를 배열로 만들지 않습니다 (나중에 필드를 추가하기 쉬움)

### 실패

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "TICKET_NOT_FOUND",
    "message": "기다림을 찾을 수 없습니다."
  }
}
```

검증 실패(400)일 때는 어떤 필드가 틀렸는지 함께 보냅니다.

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_INVALID_INPUT",
    "message": "입력값이 올바르지 않습니다.",
    "fieldErrors": [
      { "field": "nickname", "reason": "닉네임은 1~12자여야 합니다." }
    ]
  }
}
```

## 4. 에러 코드

- 형식: **`{도메인}_{이유}`**, 대문자 스네이크 케이스
- `ErrorCode` enum 하나에 모아서 관리합니다. 각 코드는 HTTP 상태 코드와 메시지를 가집니다
- **REST와 WebSocket이 같은 에러 코드를 씁니다**

| code | HTTP | 설명 |
|---|---|---|
| `COMMON_INVALID_INPUT` | 400 | 요청 값 검증 실패, JSON 형식 오류 |
| `COMMON_NOT_FOUND` | 404 | 없는 경로 |
| `COMMON_METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP 메서드 |
| `COMMON_INTERNAL_ERROR` | 500 | 알 수 없는 서버 오류 |
| `AUTH_LOGIN_REQUIRED` | 401 | 토큰이 없음, 게스트가 로그인 전용 API 호출 |
| `AUTH_INVALID_TOKEN` | 401 | 계정 토큰이 만료·위조됨 (프론트는 토큰을 지우고 다시 요청) |
| `AUTH_FORBIDDEN` | 403 | 권한 없음 |
| `ROOM_NOT_FOUND` | 404 | 없는 대기실 |
| `SITUATION_CHIP_NOT_FOUND` | 404 | 없거나 다른 방의 상황 칩 |
| `USER_NOT_FOUND` | 404 | 없는 계정 |
| `TICKET_NOT_FOUND` | 404 | 없는 기다림 |
| `TICKET_FORBIDDEN` | 403 | 내 기다림이 아님 (userId·guestToken 불일치) |
| `TICKET_ALREADY_ENDED` | 409 | 이미 종료된 기다림 |
| `TICKET_NOT_ENDED` | 409 | 아직 끝나지 않은 기다림 (옮기기 불가) |
| `TICKET_ALREADY_CLAIMED` | 409 | 이미 계정으로 옮긴 기다림 |
| `BLANKET_PIECE_ALREADY_EXISTS` | 409 | 이 기다림의 담요 조각이 이미 있음 |
| `BROADCAST_COOLDOWN` | 429 | 방송 30초 쿨다운 중 |

> 위 표는 예시입니다. 새 코드가 필요하면 `global/` 담당자에게 요청하고, 추가되면 이 표도 같이 갱신합니다.

## 5. 날짜 · 시간

- REST 응답의 시각은 **ISO-8601 + 타임존 오프셋**: `"2026-11-11T21:30:00+09:00"`
- WebSocket 메시지의 시각은 **epoch milliseconds**(`serverTs`): 실시간 계산이 편하기 때문입니다
- **경과 시간(머문 시간, 지나간 열차 수)은 서버가 "시작 시각"을 주고 프론트가 계산**하는 것을 기본으로 합니다
  - 사용자 기기 시계가 틀릴 수 있으므로, 시작 시각을 주는 응답에는 서버 현재 시각(`serverTime`)도 함께 넣습니다
- 기간은 필드 이름에 단위를 붙입니다: `cooldownMs`, `retryAfterMs`

## 6. Swagger 작성 규칙

Swagger는 프론트가 보는 API 문서입니다. **API를 만들면 Swagger 설명도 같이 작성해야 완료입니다.**

| 위치 | 어노테이션 | 예시 |
|---|---|---|
| Controller 클래스 | `@Tag` | `@Tag(name = "Ticket", description = "승차권·기다림 API")` |
| Controller 메서드 | `@Operation` | `@Operation(summary = "승차권 발급")` |
| DTO 필드 | `@Schema` | `@Schema(description = "승차권 번호", example = "104")` |

- `summary`는 짧은 한국어, 동작이 복잡하면 `description`에 규칙을 적습니다 (예: "30초에 한 번만 가능")
- `example`은 실제로 나올 법한 값을 넣습니다

## 7. WebSocket (STOMP) 메시지

Swagger는 WebSocket을 문서화하지 못하므로, 메시지 명세는 README의 **메시지 명세** 표로 관리합니다.

### 경로

| 구분 | 경로 | 예시 |
|---|---|---|
| 연결 | `/ws` | `ws://{서버}/ws` |
| 클라이언트 → 서버 | `/app/rooms/{roomId}/{동작}` | `/app/rooms/1/broadcast` |
| 방 전체 수신 | `/topic/rooms/{roomId}` | |
| 나에게만 수신 | `/user/queue/events` | 에러, 입장 직후 전체 상태 |

- `{동작}`은 소문자 한 단어 또는 kebab-case: `join`, `broadcast`, `reaction`
- 플레이어 이동(`move`)은 MVP 범위 밖이라 만들지 않습니다

### 메시지 형식

- 서버가 보내는 **모든 메시지에 `type`과 `serverTs`** 가 있습니다
- `type`은 대문자 스네이크 케이스, `MessageType` enum에 정의합니다: `JOIN`, `LEAVE`, `BROADCAST`, `REACTION`, `ERROR` ...
- 에러는 요청한 사람에게만 `/user/queue/events`로 보냅니다

```json
{
  "type": "ERROR",
  "code": "BROADCAST_COOLDOWN",
  "message": "방송은 30초에 한 번만 할 수 있습니다.",
  "retryAfterMs": 29732,
  "serverTs": 1762864200000
}
```

### 메시지를 추가·변경할 때

1. `MessageType`에 type 추가
2. README 메시지 명세 표 갱신
3. **같은 PR 안에서** 1, 2를 함께 하고, 프론트에 변경 사항을 공유합니다

## 8. API를 바꿀 때

- 이미 프론트가 쓰고 있는 API의 **필드 이름·타입을 바꾸거나 지우는 것**은 먼저 프론트와 합의합니다
- 필드를 **추가**하는 것은 바로 해도 됩니다
- 변경한 PR 설명에 "API 변경" 이라고 적고, 프론트 채널에 링크를 공유합니다

## 9. 인증

| 구분 | 헤더 | 언제 받나 |
|---|---|---|
| 계정 토큰 | `Authorization: Bearer {accessToken}` | 가입·로그인 응답. **7일** 뒤 만료, 리프레시 토큰 없음 |
| 게스트 토큰 | `X-Guest-Token: {guestToken}` | 비로그인 상태의 승차권 발급 응답. JWT가 아닌 랜덤 문자열 |

- 로그아웃은 프론트가 계정 토큰을 지우는 것으로 처리합니다 (서버 호출 없음).
- 로그인 사용자가 임시 조각을 옮길 때처럼 **두 헤더를 같이** 보낼 수 있습니다.
- 경로별 규칙

| 경로 | 필요한 것 |
|---|---|
| `GET /api/v1/rooms/**`, `GET /api/v1/tickets/stats`, `POST /api/v1/users`, `POST /api/v1/users/login`, `POST /api/v1/tickets`, Swagger, `/actuator/health`, `/ws/**` | 없음 |
| `/api/v1/users/me/**` | 계정 토큰 |
| 그 밖의 `/api/**` | 계정 토큰 또는 게스트 토큰 |

- 토큰이 필요 없는 경로라도 **만료·위조된 계정 토큰을 보내면 401 `AUTH_INVALID_TOKEN`** 입니다.
- 서버 코드에서는 컨트롤러 파라미터 `@CurrentUser Requester requester`로 `userId`(없으면 null)와 `guestToken`(없으면 null)을 꺼냅니다. "이 승차권이 내 것인지"는 각 Service가 확인합니다.
