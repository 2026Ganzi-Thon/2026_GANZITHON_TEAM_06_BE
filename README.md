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

| 방향 | 경로 | type | 설명 |
|---|---|---|---|
| — | — | — | 실시간 기능 개발 시 추가 예정 |
