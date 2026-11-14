# 코드 컨벤션 (레이어드 아키텍처)

> 새 기능을 만들 때는 **4장의 예시 코드와 똑같은 모양**으로 만들면 됩니다.
> 규칙이 애매하면 혼자 정하지 말고 물어봐 주세요.

> **Spring Boot 4 / Jackson 3 사용 (패키지 `tools.jackson`).** 인터넷의 Boot 3 기준 예제를 그대로 쓰면 안 되는 경우가 있습니다.
> 예: `com.fasterxml.jackson.databind.ObjectMapper` → `tools.jackson.databind.json.JsonMapper`, `@JsonComponent` → `@JacksonComponent`,
> `spring-boot-starter-web` → `spring-boot-starter-webmvc`, 테스트 어노테이션 패키지 변경(`org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`).
> 단, `@JsonInclude` 같은 어노테이션은 그대로 `com.fasterxml.jackson.annotation` 패키지입니다. 컴파일이 안 되면 먼저 물어봐 주세요.

## 1. 패키지 구조

도메인별로 폴더를 나누고, 그 안을 레이어로 나눕니다.

```
com.ganzithon.station            ← 베이스 패키지
├── user/                        ← 사용자 (게스트, 닉네임 로그인)
│   ├── controller/   UserController
│   ├── service/      UserService
│   ├── repository/   UserRepository
│   ├── entity/       User
│   └── dto/          request/, response/
├── room/                        ← 대기실 정보 (방, 칩, NPC, 도안)
├── ticket/                      ← 승차권 (기다림, 털실 톡톡, 종료)
├── blanket/                     ← 담요 (담요 조각, 마이 양)
├── realtime/                    ← 실시간 대기실 (WebSocket, DB 없음)
└── global/                      ← 공통 코드 (담당자만 수정)
    ├── config/       Swagger, WebSocket, CORS 등 설정
    ├── security/     인증 설정
    ├── exception/    BusinessException, ErrorCode, GlobalExceptionHandler
    ├── response/     ApiResponse (공통 응답)
    └── entity/       BaseTimeEntity
```

- 각 도메인이 무엇을 맡는지, 클래스 이름을 무엇으로 할지는 [DOMAIN.md](./DOMAIN.md)의 **용어집**을 따릅니다.
- **`global/`은 담당자만 수정합니다.** 공통 코드가 필요하면 담당자에게 요청하세요.
- 새 도메인 폴더를 만들기 전에도 먼저 공유해 주세요.
- `realtime/`은 DB를 쓰지 않아서 `repository/`, `entity/` 대신 메모리 저장소와 메시지 클래스가 들어갑니다.

## 2. 레이어 규칙

### 호출 방향

```
Controller  →  Service  →  Repository  →  DB
   (DTO)        (Entity)
```

| 레이어 | 하는 일 | 하면 안 되는 일 |
|---|---|---|
| **Controller** | 요청 받기, `@Valid` 검증, Service 호출, 응답 반환 | 비즈니스 로직 작성, Repository 호출, Entity 반환 |
| **Service** | 비즈니스 로직, 트랜잭션, Entity ↔ DTO 변환 | HTTP 관련 코드(`HttpServletRequest` 등) 사용 |
| **Repository** | DB 조회·저장 | 로직 작성 |
| **Entity** | 테이블 매핑, **자기 상태를 바꾸는 메서드** | setter 노출, DTO 의존 |

### 다른 도메인의 데이터가 필요할 때

- 다른 도메인의 **Service**를 호출합니다. 다른 도메인의 **Repository를 직접 쓰지 않습니다.**
  - ✅ `TicketService` → `RoomService.getRoom(roomId)`
  - ❌ `TicketService` → `RoomRepository.findById(roomId)`
- 두 Service가 **서로를 호출하면 안 됩니다** (순환 참조). 이런 구조가 필요해 보이면 바로 공유해 주세요.
- 호출 방향은 [DOMAIN.md](./DOMAIN.md)의 **서비스 호출 방향**을 따릅니다. 예: 기다림 종료는 `TicketService`가 `BlanketService`를 호출하고, 반대로는 호출하지 않습니다.

## 3. 클래스별 규칙

### Controller

- `@RestController` + `@RequestMapping("/api/v1/{도메인 복수형}")`
- 응답은 항상 `ApiResponse`로 감쌉니다 (→ [API_CONVENTION.md](./API_CONVENTION.md))
- 요청 DTO에는 `@Valid`를 붙입니다
- Swagger 설명: 클래스에 `@Tag`, 메서드마다 `@Operation(summary = "...")`

### Service

- 클래스에 `@Transactional(readOnly = true)`, **데이터를 바꾸는 메서드에만** `@Transactional`
- 메서드 이름

| 동작 | 이름 |
|---|---|
| 생성 | `createXxx` |
| 단건 조회 | `getXxx` |
| 목록 조회 | `getXxxList` |
| 수정 | `updateXxx` |
| 삭제 | `deleteXxx` |
| 상태 변경 | 의미가 드러나는 동사 (`endTicket`, `deliverKnitting`) |

- 없는 데이터는 `Optional`을 반환하지 말고 예외를 던집니다:
  `.orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND))`
- 다른 도메인 Service가 Entity를 받아야 할 때를 위해 `getXxxEntity(id)` 같은 메서드를 따로 둘 수 있습니다. 이 메서드는 Controller에서 쓰지 않습니다.

### Entity

- `@Setter` 금지. 상태 변경은 **의미 있는 메서드**로 합니다.
  - ❌ `ticket.setStatus(ENDED); ticket.setEndedAt(now);`
  - ✅ `ticket.end(endReason);`
- 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- 객체 생성은 생성자에 `@Builder`를 붙여서 합니다 (클래스에 붙이지 않음)
- 생성·수정 시각이 필요한 엔티티는 `BaseTimeEntity`를 상속합니다
- enum 컬럼은 반드시 `@Enumerated(EnumType.STRING)` (ORDINAL 금지)
- 연관관계는 `@ManyToOne(fetch = FetchType.LAZY)`만 기본으로 씁니다. `@OneToMany`가 필요하면 먼저 공유해 주세요.
- 테이블·컬럼 이름은 [ERD.md](./ERD.md)를 따릅니다

### DTO

- **`record`** 로 만듭니다
- 이름

| 용도 | 이름 | 위치 |
|---|---|---|
| 요청 | `XxxCreateRequest`, `XxxUpdateRequest` | `dto/request/` |
| 응답 | `XxxResponse`, `XxxListResponse` | `dto/response/` |

- Entity → 응답 DTO 변환은 응답 DTO의 `static from(Entity)` 메서드로 합니다
- 검증 어노테이션(`@NotNull`, `@NotBlank`, `@Size`)은 요청 DTO에 붙입니다
- Swagger 설명: 필드에 `@Schema(description = "...", example = "...")`

### 예외

- 비즈니스 예외는 **`BusinessException` 하나만** 씁니다. 새 예외 클래스를 만들지 않습니다.
- 에러 종류는 `ErrorCode` enum에 추가합니다 (`global/` 담당자에게 요청)
- `try-catch`로 예외를 잡아서 무시하지 않습니다. 처리는 `GlobalExceptionHandler`가 합니다.

### 그 외

- 의존성 주입은 **생성자 주입**만 (`@RequiredArgsConstructor` + `private final`). `@Autowired` 필드 주입 금지
- 로그는 `@Slf4j`의 `log.info / log.warn / log.error`. `System.out.println` 금지
- 매직 넘버는 상수로: `private static final Duration BROADCAST_COOLDOWN = Duration.ofSeconds(30);`

### Lombok 허용 범위

| ✅ 사용 | ❌ 금지 |
|---|---|
| `@Getter` | `@Data` |
| `@RequiredArgsConstructor` | `@Setter` |
| `@NoArgsConstructor(access = PROTECTED)` | Entity에 `@AllArgsConstructor` |
| 생성자에 `@Builder` | Entity에 `@ToString`, `@EqualsAndHashCode` |
| `@Slf4j` | |

### 네이밍 · 포맷

- 클래스 `PascalCase`, 메서드·변수 `camelCase`, 상수 `UPPER_SNAKE_CASE`
- boolean은 `is`, `has`로 시작: `isPublic`, `hasDelivered`
- 축약하지 않기: `tkt` ❌ → `ticket` ✅
- 포맷은 IntelliJ 기본 설정. **커밋 전에 `Ctrl + Alt + L`**(Mac: `⌘ + ⌥ + L`)로 정렬
- 안 쓰는 import는 `Ctrl + Alt + O`(Mac: `⌃ + ⌥ + O`)로 정리

## 4. 예시 코드 (이 모양 그대로 만들기)

`room` 도메인의 **대기실 목록 조회**(`GET /api/v1/rooms`)와 **단건 조회**(`GET /api/v1/rooms/{roomCode}`)가 실제로 들어 있는 기준 코드입니다.
아래는 요약이고, 전체는 `src/main/java/com/ganzithon/station/room/`의 파일을 열어 보세요. 테스트는 `src/test/.../room/controller/RoomControllerTest.java`입니다.

| 파일 | 위치 |
|---|---|
| Entity | `room/entity/Room.java` (3번에서 만듦) |
| Repository | `room/repository/RoomRepository.java` |
| 응답 DTO | `room/dto/response/RoomResponse.java`, `RoomListResponse.java` |
| Service | `room/service/RoomService.java` |
| Controller | `room/controller/RoomController.java` |
| 테스트 | `room/controller/RoomControllerTest.java` |

### Entity — `room/entity/Room.java`

```java
@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 100)
    private String slogan;

    // ... 회고 질문, NPC 문구, sortOrder

    @Builder
    private Room(String code, String name, String slogan, ...) {
        this.code = code;
        this.name = name;
        this.slogan = slogan;
    }

    // 상태를 바꿔야 하면 setter 대신 의미 있는 메서드로 (room은 시드라 없음)
}
```

### Repository — `room/repository/RoomRepository.java`

```java
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);

    List<Room> findAllByOrderBySortOrderAsc();
}
```

### 응답 DTO — `room/dto/response/RoomResponse.java`, `RoomListResponse.java`

```java
public record RoomResponse(
        @Schema(description = "대기실 코드. 단건 조회 경로에 씁니다", example = "RESULT")
        String roomCode,

        @Schema(description = "대기실 이름", example = "결과발표")
        String name,

        @Schema(description = "대기실 상단 슬로건", example = "결과를 기다리는 자리, 잠시 마음을 가만히 내려놓아도 돼요.")
        String slogan
        // ...
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getCode(), room.getName(), room.getSlogan() /* ... */);
    }
}

// 목록은 data를 배열로 만들지 않고 필드로 감쌉니다: "data": { "rooms": [ ... ] }
public record RoomListResponse(
        @Schema(description = "대기실 목록 (화면 표시 순서)")
        List<RoomResponse> rooms
) {
    public static RoomListResponse from(List<Room> rooms) {
        return new RoomListResponse(rooms.stream().map(RoomResponse::from).toList());
    }
}
```

### Service — `room/service/RoomService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;

    // API용 (Controller가 호출)
    public RoomListResponse getRoomList() {
        return RoomListResponse.from(getRoomEntityList());
    }

    public RoomResponse getRoom(String roomCode) {
        return RoomResponse.from(getRoomEntity(roomCode));
    }

    // 다른 도메인 Service용 (Controller에서 쓰지 않음)
    public Room getRoomEntity(String roomCode) {
        return roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    public List<Room> getRoomEntityList() {
        return roomRepository.findAllByOrderBySortOrderAsc();
    }
}
```

### Controller — `room/controller/RoomController.java`

```java
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
}
```

### 로그인한 사람이 필요한 API라면

컨트롤러 파라미터에 `@CurrentUser Requester requester`를 추가하고, Service에 넘겨서 소유자를 확인합니다 (→ [API_CONVENTION.md](./API_CONVENTION.md) 9장).

```java
@GetMapping("/me/blanket-pieces")
public ResponseEntity<ApiResponse<BlanketPieceListResponse>> getMyBlanketPieces(@CurrentUser Requester requester) {
    return ResponseEntity.ok(ApiResponse.success(blanketService.getMyBlanketPieceList(requester.requireUserId())));
}
```

### 테스트 — `room/controller/RoomControllerTest.java`

```java
@Import(TestcontainersConfiguration.class)   // Docker로 Postgres를 띄워서 시드 데이터까지 들어간 DB로 테스트
@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 없는_대기실은_404_ROOM_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomCode}", "NOT_EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));
    }
}
```

## 5. 실시간(WebSocket) 코드

실시간 기능은 데모 서버에서 검증한 STOMP 구조를 그대로 씁니다. 메시지 형식은 [API_CONVENTION.md](./API_CONVENTION.md)의 WebSocket 부분을 보세요.

- 실시간 코드는 **모두 `realtime/` 패키지**에 둡니다. 다른 도메인에 `@MessageMapping`을 만들지 않습니다
- STOMP 핸들러(`@MessageMapping`)도 Controller입니다. 이름은 `XxxMessageController`
- 메시지를 보낼 때는 `SimpMessagingTemplate`을 직접 쓰지 말고 `realtime/`의 공통 전송 클래스를 씁니다
- 비즈니스 로직은 REST와 똑같이 Service에 둡니다. 승차권 확인처럼 다른 도메인 데이터가 필요하면 그 도메인의 Service를 호출합니다
- 주기 작업(`@Scheduled`)을 추가하려면 먼저 공유해 주세요. 스케줄러 스레드 수가 정해져 있습니다
- **플레이어 이동은 MVP 범위 밖입니다.** 위치 필드, `move` 메시지를 미리 만들지 않습니다

## 6. 스키마 변경 규칙

DB 스키마는 Flyway가 `src/main/resources/db/migration`의 SQL 파일로 만듭니다. JPA는 `ddl-auto: validate`라서 **엔티티만 고치면 서버가 뜨지 않습니다.**

- **스키마 변경(테이블·컬럼·제약 추가, 변경)은 환희에게 요청합니다.** 직접 SQL 파일을 만들지 않습니다.
- **이미 올라간 파일은 고치지 않습니다.** 한 번 적용된 파일을 고치면 다른 사람 DB와 배포 서버에서 Flyway 검증이 실패합니다. 바꿀 내용은 항상 새 파일로 추가합니다.
- 파일 이름
  - 처음 두 개: `V1__init.sql`(스키마), `V2__seed.sql`(시드 데이터)
  - 그 이후: **`V{yyyyMMdd}_{HHmm}__{설명}.sql`** (예: `V20261116_1430__add_xxx.sql`). 만든 시각을 쓰면 여러 명이 동시에 만들어도 번호가 겹치지 않습니다
  - `V` 뒤 버전과 설명 사이는 **밑줄 두 개(`__`)**, 설명은 영어 소문자 + 밑줄
- 시드 문구(대기실 슬로건, 상황 칩 문구) 수정도 새 파일에 `UPDATE`로 씁니다. 문구를 코드에 하드코딩하지 않습니다.
- 로컬 DB가 꼬였으면 `docker compose down -v`로 지우고 다시 띄우면 처음부터 다시 만들어집니다.
