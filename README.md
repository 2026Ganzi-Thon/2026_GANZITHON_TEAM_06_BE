# 잠시 정차합니다 · 실시간 대기실 데모 서버

2D 대기실에서 캐릭터 이동 / 입장·퇴장 / 조합형 방송 / 리액션을 실시간으로 동기화하는 **데모 백엔드**입니다.
기능명세 확정 전에 "최대 180명 동시 접속에서 실시간 기능이 버티는가"를 확인하는 것이 목적입니다.

- Java 21 · Spring Boot 3.5 · Gradle · STOMP over 네이티브 WebSocket (SockJS 없음) · Simple Broker
- DB 없음: 모든 상태는 메모리(`ConcurrentHashMap`). 서버를 재시작하면 초기화됩니다.

## 실행 방법

```bash
# 서버 (http://localhost:8080)
./gradlew bootRun
# 또는
./gradlew bootJar && java -jar build/libs/milestone-0.0.1-SNAPSHOT.jar
```

- 확인용 페이지: http://localhost:8080/ (방 선택: `/?room=room-2`). 탭 2~3개를 띄우고 캔버스를 클릭하면 이동이 서로 보입니다.
- 허용 Origin: `http://localhost:5173` (Vite). 바꾸려면 `application.yml`의 `app.cors.allowed-origins` 수정 (쉼표로 여러 개).
- Actuator: `/actuator/health`, `/actuator/metrics/process.cpu.usage`, `/actuator/metrics/jvm.memory.used`

### 부하 테스트

```bash
cd load-test
npm install
node load-test.js --bots 180 --rooms 1          # 단일 실행
node run-scenarios.js                            # 30/90/180/300명 x (1개 방 | 3개 방), 결과는 load-test/results/
```

| 옵션 | 기본값 | 설명 |
|---|---|---|
| `--bots` | 30 | 봇 수 |
| `--rooms` | 1 | 방 개수. 봇을 `room-1..N`에 균등 분배 |
| `--rate` | 5 | 봇당 초당 move 전송 횟수 |
| `--duration` | 60 | 측정 시간(초). 전원 접속 완료 후부터 측정 |
| `--ramp` | 3000 | 접속을 나눠 거는 시간(ms) |
| `--workers` | 봇 40명당 1 | 봇을 나눠 돌리는 worker thread 수 (클라이언트 쪽 병목 방지) |
| `--url` | http://localhost:8080 | 서버 주소 |
| `--json` | - | 결과 JSON 저장 경로 |

봇 동작: 연결 → `/topic/rooms/{id}`, `/user/queue/events` 구독 → join → 랜덤 목표점으로 걸으며 move 전송.
1초마다 1/90 확률로 방송 시도(쿨다운 준수), BROADCAST 수신 시 30% 확률로 0~1초 뒤 리액션.

**지연 측정**: move를 보낸 시각을 seq별로 기록하고, SNAPSHOT에 자기 seq가 돌아온 시각과의 차이를 잽니다.
같은 틱(100ms) 안에서 더 새로운 seq로 덮인 move는 서버 설계상 따로 오지 않으므로 표본에서 빠집니다(`coalescedSeqs`로 집계).
즉 지연 = **틱 대기(0~100ms) + 서버 처리 + 전송 + 클라이언트 파싱**이며, 틱 구조상 평균 약 50ms가 바닥입니다.

---

## 메시지 명세 (프론트 전달용)

### 연결

```js
import { Client } from '@stomp/stompjs';
const client = new Client({ brokerURL: 'ws://localhost:8080/ws' });
client.onConnect = () => {
  client.subscribe('/user/queue/events', onMessage);        // 나에게만 오는 메시지 (반드시 join 전에 구독)
  client.subscribe(`/topic/rooms/${roomId}`, onMessage);    // 방 전체 메시지
  client.publish({ destination: `/app/rooms/${roomId}/join` });
};
client.activate();
```

- 플레이어 ID = 서버의 STOMP 세션 ID. 입장 직후 받는 `SNAPSHOT(full)`의 `selfId`가 내 ID입니다.
- 재연결하면 새 세션 = 새 ID 입니다. (인증 없음, 데모 범위)
- **STOMP heartbeat**: 서버는 `heart-beat:10000,10000`을 제안합니다. stompjs 기본값(10초/10초)이면 따로 설정할 필요가 없습니다.
  클라이언트가 heartbeat를 협상한 뒤 약 **30초**(max(클라이언트 송신 간격, 10초) × 3) 동안 아무것도 보내지 않으면 서버가 연결을 끊고 `LEAVE`를 보냅니다.
  (bfcache에서 멈춘 페이지, 조용히 끊긴 네트워크 대비. 실측: 30.6초 뒤 LEAVE)
  클라이언트가 heartbeat를 `0`으로 끄면 이 정리 기능도 꺼집니다.
- 맵 좌표계: x 0~800, y 0~600 (범위 밖 값은 서버가 clamp)

### 클라이언트 → 서버 (`/app` prefix)

| destination | body | 설명 |
|---|---|---|
| `/app/rooms/{roomId}/join` | 없음 / `{}` | 입장. 랜덤 좌표에 생성. 다른 방에 있었으면 그 방에서 퇴장 처리 후 입장 |
| `/app/rooms/{roomId}/move` | `{ "x": 120, "y": 340, "seq": 17 }` | 위치. `seq`는 클라이언트가 1씩 증가. **초당 10회 이하로 throttle 권장** |
| `/app/rooms/{roomId}/broadcast` | `{ "situationChipId": "SUBWAY_DELAY", "stateChipId": "TIRED" }` | 조합형 방송 요청 (세션당 30초 쿨다운) |
| `/app/rooms/{roomId}/reaction` | `{ "broadcastId": "…", "type": "NOD" }` | 리액션. `type`: `NOD` \| `AWW` |

`roomId`: 영문/숫자/`-`/`_` 1~40자. 데모 칩은 `room-1`~`room-3`에만 정의되어 있습니다.

### 서버 → 클라이언트

모든 메시지에 `type`이 있습니다: `JOIN` `LEAVE` `SNAPSHOT` `BROADCAST` `REACTION` `ERROR`. `serverTs`는 epoch ms.

| type | 채널 | 예시 |
|---|---|---|
| `SNAPSHOT` (full) | `/user/queue/events` | `{"type":"SNAPSHOT","full":true,"selfId":"a1b2…","serverTs":…,"players":[{"id":"a1b2…","x":91.3,"y":296.1,"seq":0}, …]}` — 입장 직후 1회, 방 전체 플레이어 (나 포함) |
| `SNAPSHOT` (delta) | `/topic/rooms/{roomId}` | `{"type":"SNAPSHOT","serverTs":…,"players":[{"id":"…","x":800,"y":0,"seq":5}]}` — 100ms마다, **이번 틱에 움직인 플레이어만**. 아무도 안 움직였으면 오지 않음 |
| `JOIN` | `/topic/rooms/{roomId}` | `{"type":"JOIN","player":{"id":"…","x":199.2,"y":193.3,"seq":0},"count":2,"serverTs":…}` |
| `LEAVE` | `/topic/rooms/{roomId}` | `{"type":"LEAVE","playerId":"…","count":1,"serverTs":…}` — 연결 종료(탭 닫기 포함) 시 |
| `BROADCAST` | `/topic/rooms/{roomId}` | `{"type":"BROADCAST","broadcastId":"e079…","senderId":"…","situationChipId":"SUBWAY_DELAY","situationLabel":"열차가 지연되고 있어요","stateChipId":"TIRED","stateLabel":"피곤해요","queuedAt":…,"serverTs":…}` |
| `REACTION` | `/topic/rooms/{roomId}` | `{"type":"REACTION","broadcastId":"e079…","playerId":"…","reaction":"NOD","serverTs":…}` — 저장하지 않고 즉시 전파 |
| `ERROR` | `/user/queue/events` | `{"type":"ERROR","code":"COOLDOWN","message":"방송은 30초에 한 번만 할 수 있습니다.","retryAfterMs":29732,"serverTs":…}` |

`ERROR.code`

| code | 상황 |
|---|---|
| `NOT_JOINED` | 입장하지 않은 방에 move/broadcast/reaction |
| `INVALID_ROOM` | roomId 형식 오류 |
| `INVALID_CHIP` | 그 방에 없는 상황 칩, 또는 없는 상태 칩 |
| `COOLDOWN` | 30초 쿨다운 중. `retryAfterMs` 포함 |
| `QUEUE_FULL` | 방 방송 대기열(최대 20건)이 가득 참 |
| `BAD_REQUEST` | JSON 파싱 실패, 필수 필드 누락, 잘못된 enum |

### 방송 규칙
- 세션당 30초 쿨다운. 첫 방송은 바로 가능. 위반 시 요청자에게만 `ERROR(COOLDOWN)`.
- 방송은 방별 대기열에 들어가고, **방마다 최소 15초 간격**으로 하나씩 `BROADCAST`로 송출됩니다.
  대기열이 비어 있고 최근 15초간 송출이 없었으면 0.5초 안에 바로 나갑니다.
- 접수 성공 시 별도 ACK는 없습니다 (내 방송은 `BROADCAST.senderId`로 확인).
- 대기열 상한 20건: 상한이 없으면 30초 쿨다운 × 인원수가 15초당 1건 송출보다 빨라 대기열이 끝없이 늘어납니다 (명세 외 안전장치).

### 데모 칩 목록

| 방 | 상황 칩 |
|---|---|
| room-1 (지하철) | `SUBWAY_DELAY` 열차가 지연되고 있어요 · `SUBWAY_CROWDED` 승강장이 붐벼요 · `SUBWAY_TRANSFER` 환승 기다리는 중 |
| room-2 (병원) | `HOSPITAL_WAITING` 진료 순서를 기다려요 · `HOSPITAL_RESULT` 검사 결과 기다리는 중 · `HOSPITAL_PHARMACY` 약 타러 가는 중 |
| room-3 (버스) | `BUS_LATE` 버스가 늦게 와요 · `BUS_RAIN` 비가 와요 · `BUS_LAST` 막차를 기다려요 |
| 공통 상태 칩 | `TIRED` 피곤해요 · `BORED` 심심해요 · `HAPPY` 기분 좋아요 · `HUNGRY` 배고파요 |

### 프론트 구현 팁 (확인용 페이지 `static/index.html` 참고)
- 내 캐릭터는 로컬에서 바로 움직이고, 위치 전송만 100ms throttle. delta SNAPSHOT 안의 내 항목은 무시해도 됩니다.
- 다른 캐릭터는 SNAPSHOT 좌표를 목표값으로 두고 `requestAnimationFrame`에서 lerp (`k = 1 - exp(-dt*12)`).
- delta SNAPSHOT에는 움직인 사람만 있으므로 **병합(upsert)** 해야 합니다. 제거는 `LEAVE`로만 합니다.
- **연결은 반드시 직접 끊으세요.** 서버는 WebSocket이 실제로 닫혀야 퇴장 처리합니다.
  - Chrome은 페이지 이동 시 이전 페이지를 bfcache에 넣으면서 WebSocket을 열어 둘 수 있습니다 (확인용 페이지에서 실제로 발생: 방을 옮겼다 돌아오면 이전 내 캐릭터가 남음).
    → `pagehide`에서 `client.deactivate({ force: true })`를 호출합니다.
  - React에서는 `useEffect` cleanup에서 `client.deactivate()`를 호출합니다. 그러지 않으면 방 전환이나 StrictMode 이중 마운트 때마다 유령 연결이 쌓입니다.

---

## 구조

```
hwan.milestone
├─ config/      WebSocketConfig          /ws, /app, simple broker(/topic, /queue), CORS, 세션 내 순서 보장, heartbeat 10s
├─ controller/  RoomMessageController    @MessageMapping join/move/broadcast/reaction, 예외 → ERROR
├─ websocket/   RoomMessenger            방 토픽 / 세션 개인 큐 전송
│               WebSocketEventListener   SessionDisconnectEvent → 퇴장
│               SnapshotTicker           @Scheduled(fixedRate=100) dirty 플레이어 → SNAPSHOT
│               BroadcastDispatcher      @Scheduled(fixedRate=500) 방별 15초 간격 송출
├─ service/     RoomService, BroadcastService, ChipCatalog, GameException
├─ repository/  PlayerRepository, BroadcastRepository   (ConcurrentHashMap)
├─ domain/      Player(불변 Position + AtomicBoolean dirty), QueuedBroadcast
└─ dto/         요청/이벤트 record, MessageType
```

- move는 받는 즉시 방송하지 않고 최신 좌표만 덮어쓰고 dirty 표시 → 틱에서 방마다 SNAPSHOT 1개.
  N명이 초당 5회 움직여도 방당 송신은 초당 최대 10회 × N명 수신으로 고정됩니다.
- 인증이 없어 Principal이 없으므로, 개인 메시지는 sessionId 헤더로 user destination을 해석시킵니다.
- `setPreserveReceiveOrder(true)`: 한 세션의 SUBSCRIBE → join, move seq 순서가 뒤집히지 않게.

---

## 부하 테스트 결과

### 환경
- Windows 11 · 논리 코어 20개 · Java 21 (힙 옵션 기본값) · Node 24.15
- 서버와 봇이 **같은 PC(localhost)** 에서 동작합니다. 실제 네트워크 지연은 빠져 있고, 봇이 서버와 CPU를 나눠 씁니다.
- 봇당 move 5회/s, 측정 60초 (접속 완료 후부터), 실행: `node run-scenarios.js`

### 결과

| 인원 | 배치 | 연결 성공/실패 | p50 | p95 | p99 | max | 수신 msg/s (봇당) | 수신 KB/s | CPU avg / max | JVM mem avg / max (MB) | heap 시작→끝 (MB) | 판정 |
|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|:---:|
| 30 | 한 방에 몰림 | 30 / 0 | 50 | 99 | 113 | 137 | 319.7 (10.7) | 346.7 | 0.2% / 1.4% | 211.4 / 297.7 | 129.7 → 125.4 | ✅ |
| 30 | 3개 방 분산 | 30 / 0 | 50 | 98 | 112 | 120 | 307.9 (10.3) | 126.8 | 0.2% / 1.1% | 212.9 / 298.2 | 174.2 → 136.1 | ✅ |
| 90 | 한 방에 몰림 | 90 / 0 | 52 | 104 | 113 | 122 | 1065.5 (11.8) | 3016.6 | 1.1% / 4.5% | 215.9 / 299.6 | 156.5 → 154.8 | ✅ |
| 90 | 3개 방 분산 | 90 / 0 | 61 | 104 | 113 | 129 | 959.2 (10.7) | 1039.3 | 0.7% / 3.6% | 220.1 / 304.5 | 128.4 → 147.7 | ✅ |
| **180** | **한 방에 몰림** | 180 / 0 | 63 | **107** | 116 | 133 | 2454.6 (13.6) | 11956.7 | 1.9% / **10.9%** | 241.2 / 323.5 | 152.4 → 141 | ✅ |
| **180** | **3개 방 분산** | 180 / 0 | 54 | **101** | 114 | 131 | 2034.1 (11.3) | 4056.8 | 1.3% / 8.9% | 222.5 / 326.3 | 173.9 → 155.5 | ✅ |
| 300 | 한 방에 몰림 | 300 / 0 | 70 | 114 | 124 | 149 | 4828.8 (16.1) | 33200.8 | 6.2% / 20.5% | 207.7 / 299 | 181.5 → 168.2 | ✅ |
| 300 | 3개 방 분산 | 300 / 0 | 63 | 103 | 115 | 126 | 3626.6 (12.1) | 11169.9 | 3.6% / 10.1% | 214.4 / 322.9 | 144.3 → 169.7 | ✅ |

지연 단위 ms · 기준: p95 ≤ 200ms, CPU ≤ 70% · 원본: `load-test/results/*.json`, `summary.md`
모든 시나리오에서 측정 중 끊긴 연결은 0개입니다. 수신 메시지 중 ERROR는 봇이 받은 `QUEUE_FULL`/`COOLDOWN`으로, 의도된 동작입니다.

### 해석
- **지연**: 모든 시나리오가 p95 98~114ms로 기준(200ms)의 절반 수준입니다. 지연의 대부분은 100ms 틱을 기다리는 시간(평균 50ms, 최대 100ms)이고,
  300명이 한 방에 몰려도 틱 대기 외 추가 지연은 p95 기준 약 10ms입니다.
- **CPU**: `process.cpu.usage`는 **20코어 전체 대비** 비율입니다. 180명·한 방은 평균 1.9%(약 0.4코어), 최대 10.9%(약 2.2코어)입니다.
  최대치는 접속이 몰리는 구간의 순간값입니다. **2 vCPU 서버로 환산하면 최대치가 100% 근처**라서, 배포할 인스턴스 크기에서 한 번 더 측정해야 합니다.
- **메모리**: 실행 중 heap은 GC 톱니 모양으로 오르내리며 꾸준히 늘지 않았습니다.
  전체 시나리오(누적 접속 약 2,000회)가 끝난 뒤 유휴 상태에서 GC를 강제하면 **heap 27.5MB**였고, `Player` 인스턴스는 남지 않았습니다. → 메모리는 안정적입니다.
- **대역폭**: 기준 항목은 아니지만 실제 배포에서 가장 먼저 걸릴 수 있는 부분입니다. 한 방 인원 N명이 모두 움직이면 서버 송신량은 대략 N²에 비례합니다.
  180명·한 방은 약 **12MB/s(≈ 96Mbps)**, 300명·한 방은 약 33MB/s입니다.

### 개선안 (기준은 통과했지만 배포 전에 검토할 것)
1. **페이로드 줄이기**: 플레이어 ID가 UUID(36자)라 SNAPSHOT 크기의 절반 가까이를 차지합니다. 방 안에서 쓰는 짧은 숫자 ID로 바꾸고, 좌표를 정수로 보내면 대역폭이 대략 절반으로 줄어들 것으로 추정합니다 (측정하지 않음).
2. **방 인원 상한**: 송신량이 N²에 비례하므로 한 방 인원 상한을 두거나(예: 60~100명) 자동으로 방을 나누는 것이 가장 효과가 큽니다. 3개 방으로 나누면 송신량이 1/3입니다.
3. **빈 방 방송 대기열 정리**: 방이 비어도 대기열이 남습니다(테스트 후 54건, 방당 상한 20건이라 무한히 늘지는 않음). 다음 입장자가 이미 나간 사람의 방송을 받게 되므로, 방이 비면 대기열을 비우는 처리가 필요합니다.
4. 배포 대상 인스턴스에서 봇을 **다른 머신**에 두고 다시 측정해야 합니다 (실제 네트워크 지연과 CPU 여유 확인).

### 부하 테스트 클라이언트 크래시 (`0xC0000409`)
첫 번째 전체 실행에서 300명 시나리오 2개가, 두 번째 실행에서 180명·3개 방 시나리오 1개가 결과를 남기지 못하고 끝났습니다. 위 표의 값은 그 시나리오들을 다시 돌려 얻은 정상 측정값입니다.

**원인: 아직 확정하지 못했습니다.** 지금까지 확인한 사실은 다음과 같습니다.
- 죽은 것은 **부하 테스트 Node 프로세스**입니다. 서버는 그동안 계속 `UP`이었고 서버 로그에 오류가 없습니다. → 서버 문제가 아닙니다.
- 종료 코드는 `3221226505` = `0xC0000409` (STATUS_STACK_BUFFER_OVERRUN, Windows의 fail-fast 강제 종료)입니다. stderr 출력, Windows 이벤트 로그, 크래시 덤프가 모두 남지 않았습니다.
- 매번 **측정 시작 전 접속 단계**에서 죽었습니다. 측정 중이나 종료 처리 중에는 한 번도 죽지 않았습니다.
- 인원과 무관하게 산발적으로 납니다. 180명·1초 실행 기준 재현율은 20회 중 1회, 30회 중 5회였습니다.

| 실험 | 크래시 | 결론 |
|---|---|---|
| worker thread 1개로 같은 부하 | 2 / 30 | worker 동시성 문제 아님 |
| worker 5개가 `@stomp/stompjs`, `ws` import만 하고 종료 | 0 / 40 | 모듈 로딩 문제 아님 |
| worker 안 동적 `import()` → 정적 import로 변경 | 이후에도 재발 | 원인 아님 (변경은 유지) |
| 단계별 동기 stderr 로그 추가 | 0 / 40 | 타이밍에 민감 → 로그를 넣으면 재현 안 됨 |

추정: Windows용 Node 24와 동시 소켓 연결 초기화 사이의 런타임 수준 문제일 가능성이 높습니다 (검증하지 않음).
**대응**: `run-scenarios.js`는 결과 없이 죽은 시나리오만 최대 3번까지 다시 실행하고, 재시도 횟수를 결과 JSON의 `attempts`와 표의 판정 칸에 남깁니다.
크래시는 측정 시작 전에만 났으므로 재시도가 측정값을 왜곡하지 않습니다.
다시 조사한다면 Node LTS(22.x)로 실행해 비교하거나, `procdump -e -ma node.exe`로 덤프를 받아 보는 것이 다음 단계입니다.
