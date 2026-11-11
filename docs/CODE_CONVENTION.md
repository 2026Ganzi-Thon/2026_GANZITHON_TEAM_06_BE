# 코드 컨벤션 (레이어드 아키텍처)

> 새 기능을 만들 때는 **4장의 예시 코드와 똑같은 모양**으로 만들면 됩니다.
> 규칙이 애매하면 혼자 정하지 말고 물어봐 주세요.

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

`room` 도메인에서 "방 하나 조회"를 만든다고 하면 파일 5개가 생깁니다.

### Entity — `room/entity/Room.java`

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType type;

    @Column(nullable = false)
    private String name;

    private String slogan;

    @Builder
    private Room(RoomType type, String name, String slogan) {
        this.type = type;
        this.name = name;
        this.slogan = slogan;
    }

    // 상태 변경은 의미 있는 메서드로
    public void changeSlogan(String slogan) {
        this.slogan = slogan;
    }
}
```

### Repository — `room/repository/RoomRepository.java`

```java
public interface RoomRepository extends JpaRepository<Room, Long> {
}
```

### 응답 DTO — `room/dto/response/RoomResponse.java`

```java
public record RoomResponse(
        @Schema(description = "방 ID", example = "1")
        Long roomId,

        @Schema(description = "방 이름", example = "결과발표 대기실")
        String name,

        @Schema(description = "상단 슬로건", example = "오늘도 잘 기다리고 있어요")
        String slogan
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getSlogan());
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

    public RoomResponse getRoom(Long roomId) {
        return RoomResponse.from(getRoomEntity(roomId));
    }

    // 다른 도메인 Service에서 Entity가 필요할 때 사용
    public Room getRoomEntity(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }
}
```

### Controller — `room/controller/RoomController.java`

```java
@Tag(name = "Room", description = "대기실 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "대기실 단건 조회")
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoom(roomId)));
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
