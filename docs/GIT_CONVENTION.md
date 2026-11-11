# Git 협업 규칙

> 처음이라도 이 문서 순서대로만 따라 하면 됩니다. 헷갈리면 **직접 push 하기 전에** 물어봐 주세요.

## 1. 브랜치

| 브랜치 | 역할 | 직접 push |
|---|---|:---:|
| `main` | 최종 배포 브랜치. `dev`에서 검증된 것만 들어옵니다 | ❌ 금지 |
| `dev` | 통합 개발 브랜치. 모든 기능 브랜치는 여기로 PR을 올립니다 | ❌ 금지 |
| `{type}/{설명}` | 각자 맡은 기능 작업 브랜치. **항상 `dev`에서 만듭니다** | ⭕ 본인 브랜치만 |

### 브랜치 이름

```
{type}/{기능명세 ID}_{설명}
```

- `{type}`은 아래 커밋 타입과 같습니다 (`feat`, `fix`, `refactor` ...)
- `{기능명세 ID}`는 노션 기능명세서의 ID를 **그대로** 씁니다 (대문자, 하이픈 포함: `ON-04`, `BR-01`)
- `{설명}`은 **영어 소문자 + 언더스코어(`_`)**
- 예시
  - `feat/ON-04_ticket_issue`
  - `feat/BR-01_broadcast_send`
  - `fix/BR-01_broadcast_cooldown`

**ID가 애매할 때**

| 상황 | 규칙 | 예시 |
|---|---|---|
| 기능 여러 개를 한 브랜치에서 작업 | 대표 ID 하나만 쓰고, 나머지는 PR 설명에 적기 | `feat/EN-02_ticket_end` (EN-03도 같이 작업) |
| 기능명세에 없는 작업 (세팅, 공통 코드, 문서) | ID 생략 | `chore/project_setup`, `docs/api_convention` |

## 2. 커밋 메시지

```
{type}: {무엇을 했는지}
```

- 예시: `feat: 승차권 발급 API 추가`, `fix: 방송 쿨다운 계산 오류 수정`
- 설명은 **한국어**로, 50자 이내로 씁니다. 끝에 마침표를 찍지 않습니다.
- 한 커밋에는 한 가지 일만 담습니다. "이것저것 수정" 같은 커밋은 만들지 않습니다.

| type | 언제 쓰나요 |
|---|---|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작은 그대로, 코드 구조만 개선 |
| `docs` | 문서(README, md) 수정 |
| `test` | 테스트 코드 추가·수정 |
| `chore` | 빌드 설정, 의존성, 설정 파일 등 기능과 무관한 작업 |
| `hotfix` | `main`에서 급하게 고쳐야 하는 버그 |

## 3. 작업 흐름 (그대로 따라 하기)

### ⓪ 처음 한 번: clone

```bash
git clone -b dev https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_06_BE.git
```

> 지금 리포의 기본 브랜치가 `dev`가 아닙니다. **`-b dev`를 꼭 붙여서** clone 하세요.
> 이미 clone 했다면 `git checkout dev`로 dev 브랜치로 옮긴 뒤 시작합니다.

### ① 작업 시작: 최신 dev에서 브랜치 만들기

```bash
git checkout dev
git pull origin dev                     # 최신 dev 받기 (꼭!)
git checkout -b feat/ON-04_ticket_issue       # 내 작업 브랜치 생성
```

### ② 작업하면서 커밋

```bash
git status                              # 무엇이 바뀌었는지 확인
git add src/main/java/.../TicketService.java   # 필요한 파일만 add
git commit -m "feat: 승차권 발급 서비스 추가"
```

> `git add .`을 쓸 때는 바로 전에 `git status`로 **올리면 안 되는 파일**(`.env`, `application-local.yml`, `build/`)이 없는지 확인하세요.

### ③ 원격에 올리기

```bash
git push -u origin feat/ON-04_ticket_issue    # 처음 한 번만 -u, 이후엔 git push
```

### ④ PR 만들기

1. GitHub에서 **Compare & pull request** 클릭
2. **base가 `dev`인지 꼭 확인** (`main`으로 올리면 안 됩니다)
3. PR 제목은 커밋 메시지 형식에 기능명세 ID를 붙여서: `feat: [ON-04] 승차권 발급 API`
4. PR 템플릿 항목 채우기
5. 리뷰어 지정

### ⑤ 리뷰 반영

리뷰 코멘트를 고친 뒤 같은 브랜치에 커밋하고 `git push` 하면 PR에 자동으로 반영됩니다. PR을 새로 만들 필요 없습니다.

### ⑥ 머지 후 정리

```bash
git checkout dev
git pull origin dev
git branch -d feat/ON-04_ticket_issue         # 다 쓴 로컬 브랜치 삭제
```

다음 작업은 다시 ①부터 시작합니다. **머지된 브랜치를 다시 쓰지 않습니다.**

## 4. 충돌(conflict)이 났을 때

PR에 "This branch has conflicts"가 뜨면, 내 브랜치에 최신 dev를 합칩니다.

```bash
git checkout feat/ON-04_ticket_issue
git pull origin dev                     # 여기서 충돌 파일이 표시됨
```

1. IntelliJ에서 충돌 파일 열기 → **Resolve Conflicts** 창에서 양쪽 코드를 보고 합치기
2. 내 코드가 아닌 부분은 **지우지 말고 담당자에게 물어보기**
3. 다 고쳤으면

```bash
git add .
git commit -m "chore: dev 최신 반영 및 충돌 해결"
git push
```

> `git push --force`는 쓰지 않습니다. `rebase`도 익숙하지 않다면 쓰지 않습니다.

## 5. PR 규칙

- PR 하나는 기능 하나. 파일이 너무 많아지면 나눠서 올립니다.
- 승인(Approve) **최소 1명** 받은 뒤 머지합니다.
- 머지 방식: `dev`로는 **Squash and merge** (기능 하나가 커밋 하나로 정리됨)
- `dev` → `main`은 배포 시점에만 **백엔드 리드(환희)** 가 PR로 올리고 머지합니다.
- `main`, `dev`에는 브랜치 보호 규칙이 걸려 있습니다: **PR 없이 push 불가, 승인 1명 필수, force push 불가**. 규칙 때문에 push가 막히면 정상이니 PR로 올려 주세요.
- 빌드가 깨지는 PR은 머지하지 않습니다. 올리기 전에 로컬에서 `./gradlew build`가 통과하는지 확인하세요.

## 6. 절대 하지 말 것

- `main`, `dev`에 직접 push
- `git push --force` (특히 공용 브랜치)
- 비밀번호, 키, `.env`, `application-local.yml` 커밋
- `build/`, `out/`, `.gradle/` 같은 빌드 결과물 커밋
- `.idea/` 같은 개인 IDE 설정 커밋 (`.gitignore`에 들어 있습니다)
- 남의 브랜치에 허락 없이 push
