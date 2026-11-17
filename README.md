# 간이역 대기실 (2026 간지톤 6팀) — Backend

내가 바꿀 수 없는 기다림(합격 발표, 답장)을 **2D 간이역 대기실에서 다른 승객과 함께 보내는 서비스**의 백엔드입니다.
승차권을 받고 대기실에 앉아 있으면 열차가 지날 때마다 털실을 받고, 기다림이 끝나면 담요 조각 1칸을 받습니다.

- API 문서 (Swagger): `{서버 주소}/swagger-ui/index.html`
- 배포 서버: `https://api.wait-station.online`

## 기술 스택

| 구분 | 사용 |
|---|---|
| 언어 · 프레임워크 | Java 21, Spring Boot 4 (Jackson 3) |
| DB | PostgreSQL 17, Spring Data JPA, Flyway |
| 인증 | Spring Security, JWT(계정 토큰) + 게스트 토큰 |
| 실시간 | WebSocket (STOMP, 내장 Simple Broker) |
| 문서 | springdoc-openapi (Swagger) |
| 테스트 | JUnit 5, MockMvc, Testcontainers |
| 배포 | Docker Compose, Caddy, AWS EC2 · ECR · CodeDeploy, Terraform, GitHub Actions |

## 로컬 실행

**Docker Desktop을 먼저 켜 두세요.** 로컬 DB와 테스트가 모두 Docker를 씁니다.

```bash
git clone -b dev https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_06_BE.git
cd 2026_GANZITHON_TEAM_06_BE

docker compose up -d         # 로컬 Postgres
./gradlew bootRun            # http://localhost:8080 (local 프로필, 기본값으로 바로 뜸)
```

- 값을 바꾸고 싶으면 `.env.example`을 복사해 `.env`를 만듭니다 (`.env`는 커밋하지 않습니다).
- 스키마와 시드 데이터(대기실, 상황 칩, 도안)는 서버가 뜰 때 Flyway가 자동으로 넣습니다.
- 로컬 DB가 꼬였으면 `docker compose down -v`로 지우고 다시 띄웁니다.

## 빌드 · 테스트

```bash
./gradlew build    # PR 올리기 전에 통과해야 합니다
```

## 패키지 구조

| 패키지 | 맡는 것 | 담당 |
|---|---|---|
| `user` | 닉네임+PIN 가입·로그인, 담요 공개 설정 | 상진 |
| `room` | 대기실, 상황 칩, NPC, 도안 (시드 데이터) | 하빈 |
| `ticket` | 승차권(기다림 한 건), 열차·털실, 털실 톡톡, 전광판 통계 | 환희 |
| `blanket` | 담요 조각, 내 담요, 공개 카드 | 상진 |
| `realtime` | 실시간 대기실 (WebSocket, 메모리만 사용) | 환희 |
| `global` | 설정, 보안, 예외, 공통 응답 | 환희 |

## 문서

새 기능을 만들기 전에 꼭 읽어 주세요.

| 문서 | 내용 |
|---|---|
| [DOMAIN.md](docs/DOMAIN.md) | 도메인 설계, 기능명세 ID별 담당, 용어집 |
| [ERD.md](docs/ERD.md) | 테이블 설계 |
| [API_CONVENTION.md](docs/API_CONVENTION.md) | URL, 응답 형식, 에러 코드, 인증, WebSocket 메시지 형식 |
| [CODE_CONVENTION.md](docs/CODE_CONVENTION.md) | 레이어 규칙, 예시 코드, 스키마 변경 규칙 |
| [GIT_CONVENTION.md](docs/GIT_CONVENTION.md) | 브랜치, 커밋, PR 규칙 |
| [INFRA.md](docs/INFRA.md) | 인프라 · 배포 |

## 협업 규칙 요약

- 작업 브랜치는 `dev`에서 `{type}/{기능명세 ID}_{설명}`으로 만들고 (예: `feat/ON-04_ticket_issue`), PR은 `dev`로 올립니다.
- PR 제목: `feat: [ON-04] 승차권 발급 API`. 승인 1명 이상, **Squash and merge**.
- `dev`에 머지되면 GitHub Actions가 배포 서버에 자동으로 배포합니다.

## WebSocket 메시지 명세

Swagger가 WebSocket을 문서화하지 못해서 여기 표로 관리합니다 (형식은 [API_CONVENTION.md](docs/API_CONVENTION.md) 7장).
메시지를 추가·변경하는 PR에서 이 표도 함께 갱신합니다.

### 연결

```js
const client = new Client({
  brokerURL: 'wss://api.wait-station.online/ws',
  // REST와 같은 헤더를 CONNECT에 넣습니다. 로그인했으면 Authorization, 아니면 X-Guest-Token
  connectHeaders: { Authorization: `Bearer ${accessToken}`, 'X-Guest-Token': guestToken },
});
```

- 만료·위조된 계정 토큰이면 연결이 거부됩니다 (STOMP ERROR 후 종료). 토큰을 지우고 다시 연결하세요.
- 연결 후 **`/user/queue/events`와 `/topic/rooms/{roomCode}`를 먼저 구독하고** 입장(join)을 보냅니다.
- 시각은 모두 epoch ms입니다 (`serverTs`, `startedTs`).

### 클라이언트 → 서버

| 경로 | body | 설명 |
|---|---|---|
| `/app/rooms/{roomCode}/join` | `{ "ticketId": 1 }` | 입장 (ON-05). 그 대기실의 진행 중인 내 승차권이어야 합니다. 자동으로 좌석에 앉습니다 |
| `/app/rooms/{roomCode}/broadcast` | `{ "situationChipCode": "NO_REPLY_WAITING_REPLY", "moodChip": "FLUTTERING" }` | 조합형 방송 (BR-01). 입장한 방에서만, 두 칩 모두 필수, 30초에 한 번. 고른 상태 칩으로 내 호칭이 바뀝니다 |

### 서버 → 클라이언트

| type | 받는 곳 | 필드 | 언제 |
|---|---|---|---|
| `ROOM_STATE` | `/user/queue/events` | `roomCode`, `myTicketNumber`, `seatCount`, `passengers[]`, `serverTs` | 입장 직후 나에게만. 이 메시지로 화면을 처음 그립니다 |
| `JOIN` | `/topic/rooms/{roomCode}` | `passenger`, `count`, `serverTs` | 새 승객 입장 (나 포함). 같은 승차권으로 다시 접속하면 오지 않습니다 |
| `LEAVE` | `/topic/rooms/{roomCode}` | `ticketNumber`, `seatNumber`, `count`, `serverTs` | 승객 퇴장 (연결 끊김, 다른 방으로 이동, 기다림 종료). `seatNumber`는 비워진 좌석, 서 있던 승객이면 null |
| `BROADCAST_ACCEPTED` | `/user/queue/events` | `broadcastId`, `queuePosition`, `cooldownMs`, `displayName`, `serverTs` | 방송 요청이 대기열에 들어감. `cooldownMs` 동안 방송 버튼을 막아 주세요 |
| `BROADCAST` | `/topic/rooms/{roomCode}` | `broadcastId`, `ticketNumber`, `displayName`, `moodChip`, `situationChipCode`, `situationChipLabel`, `sentence`, `displayMs`, `queuedTs`, `serverTs` | 전광판 송출 (BR-02). 대기열 순서대로 `displayMs`(15초) 간격. 보낸 승객 카드의 호칭도 이 값으로 갱신하세요 |
| `ERROR` | `/user/queue/events` | `code`, `message`, `retryAfterMs`(있을 때만), `serverTs` | 요청 실패. `code`는 REST와 같은 에러 코드 |

`passenger` (`passengers[]`의 원소)

| 필드 | 설명 |
|---|---|
| `ticketNumber` | 승차권 번호. 방 안에서 승객을 구분하는 값 |
| `displayName` | 대기실 호칭. 예: `104번 두근두근한 양` |
| `moodChip` | 상태 칩. 없으면 null |
| `seatNumber` | 좌석 번호 (1부터). 자리가 다 차서 서서 기다리면 null (좌석 수: 환경변수 `SEAT_COUNT`, 기본 20) |
| `startedTs` | 기다림 시작 시각. 머문 시간 계산용 |

`sentence` 예: `안내 말씀 드립니다. 104번 두근두근한 양님은 지금 '답장 기다리는 중'입니다.`

실패 `code`
- 입장: `TICKET_NOT_FOUND`, `TICKET_FORBIDDEN`(내 승차권 아님), `TICKET_ALREADY_ENDED`, `TICKET_ROOM_MISMATCH`(다른 대기실 승차권), `COMMON_INVALID_INPUT`(형식 오류)
- 방송: `REALTIME_NOT_JOINED`(그 방에 입장 안 함), `BROADCAST_COOLDOWN`(+`retryAfterMs`), `BROADCAST_QUEUE_FULL`(방 대기열 20개 가득), `SITUATION_CHIP_NOT_FOUND`(그 방의 칩이 아님), `TICKET_ALREADY_ENDED`, `COMMON_INVALID_INPUT`(칩 누락)

> 계정 닉네임과 `ticketId`는 다른 승객에게 보내지 않습니다.
