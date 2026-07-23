# Re:Fail 리팩터링 목표 모드 실행 프롬프트

아래 프롬프트 전체를 새 작업의 목표 모드에 사용한다.

---

## 실행 프롬프트

### 최종 목표

Re:Fail의 현재 공개 동작과 데이터 계약을 유지하면서 백엔드·프론트엔드 구조를 전체적으로 점검하고 리팩터링한다. 리팩터링이 끝나면 핵심 사용자·관리자 흐름을 Playwright E2E 테스트로 자동화하는 다음 작업까지 진행한다.

단순히 파일을 나누거나 이름을 바꾸는 것이 목적이 아니다. 책임 경계, 테스트 가능성, 읽기 쉬운 코드, 일관된 오류 처리와 변경 안전성을 개선하고 그 근거를 문서와 커밋으로 남긴다.

### 현재 기준선

- 저장소: `duvwlq/refail`
- 기준 브랜치: `main`
- 기준 커밋: `521e418` 이후 최신 `origin/main`
- 백엔드: Java 21, Spring Boot 3.5, MySQL 8.4, Flyway
- 프론트엔드: Next.js 16, React 19, TypeScript
- 기준 검증: 백엔드 테스트 37개, 프론트엔드 lint·production build, Docker Compose, GitHub Actions
- 현재 운영 구성: MySQL, Spring Boot, Next.js, Prometheus, Grafana

### 반드시 지킬 원칙

1. 먼저 저장소와 연결 문서를 읽고 실제 코드를 기준으로 계획을 수정한다.
2. 사용자에게 계획만 제시하고 멈추지 말고 구현, 검증, 문서화, 커밋까지 단계별로 완료한다.
3. 각 단계는 기존 동작을 고정하는 테스트 또는 측정 기준을 먼저 확인한다.
4. 리팩터링 중 REST 경로, HTTP 상태, 오류 코드, JSON 필드, DB 스키마, 공개 화면 동작을 임의로 바꾸지 않는다.
5. 필요한 동작 변경이 발견되면 리팩터링과 섞지 말고 별도 단계와 별도 커밋으로 분리한다.
6. 성능 수치는 같은 데이터와 환경에서 개선 전후를 측정하지 않으면 README에 기록하지 않는다.
7. 사용자 변경과 무관한 파일을 되돌리거나 덮어쓰지 않는다.
8. `PostService`를 여러 파일로 기계적으로 쪼개거나 범용 `BaseService`, 과도한 추상 계층을 만들지 않는다.
9. 마이크로서비스, Redis, 메시지 큐, 검색 엔진은 실제 요구가 없으므로 도입하지 않는다.
10. 단계별 독립 커밋을 남기고 전체 완료 후 `main` CI 통과까지 확인한다.

### 시작 전 필수 확인

다음 문서를 먼저 읽는다.

- `README.md`
- `API_SPEC.md`
- `ARCHITECTURE.md`
- `HARNESS.md`
- `PERFORMANCE.md`
- `AUTH_SECURITY.md`
- `RATE_LIMIT_POLICY.md`
- `OBSERVABILITY.md`
- `BACKEND_REVIEW_RESULT.md`
- `PORTFOLIO_IMPROVEMENTS.md`
- `FEATURE_STATUS.md`

다음 명령으로 기준 상태를 확보한다.

```powershell
git status --short
git log -10 --oneline
.\gradlew.bat test --rerun-tasks
cd frontend
npm.cmd run lint
npm.cmd run build
```

작업 트리가 예상하지 못한 변경으로 더럽거나 기준 테스트가 실패하면 원인을 먼저 분석한다. 현재 작업과 직접 충돌하지 않는 사용자 변경은 보존한다.

---

## 1단계: 핵심 흐름 회귀 기준 고정

### 목표

리팩터링 전에 어떤 동작을 반드시 보존해야 하는지 테스트와 흐름표로 고정한다.

### 확인할 사용자 흐름

| 흐름 | 시작 | 성공 결과 | 주요 실패 결과 |
| --- | --- | --- | --- |
| 인증 | 회원가입 → 로그인 → 갱신 → 로그아웃 | Access Token 발급과 Refresh 회전 | 중복 계정 409, 잘못된 인증 401, 재사용 탐지 401 |
| 게시글 | 작성 → 목록 → 상세 → 수정 → 삭제 | 작성자만 변경 가능 | 비작성자 403, 삭제·숨김 글 404 |
| 후속 기록 | 실패 글 → 후속 작성 → 수정 → 삭제 | 부모 글과 성장 기록 연결 | 비작성자 403, 숨김·삭제 부모 404 |
| 탐색 | 검색 → 카테고리·크기 필터 → 정렬 → 페이지 이동 | 공개 글만 일관된 결과 | 입력 상한 초과 400 |
| 공감 | 공감 생성 → 변경 → 취소 | 유형별 집계 일치 | 본인 글 403, 경쟁 충돌 계약 유지 |
| 신고 | 신고 생성 → 중복 신고 | 신고와 집계 일치 | 중복 신고 409, 본인 글 정책 유지 |
| 관리자 | 신고 조회 → 숨김 → 복구 → 사용자 제한 | 처리 이력과 공개 상태 일치 | 비관리자 403, 제한 관리자 403 |
| 관측성 | API 요청 → Request ID → Prometheus 수집 | 로그·메트릭 연결 | 관리 메트릭 외부 노출 금지 |

### 작업

1. 현재 통합 테스트가 위 흐름을 어디까지 검증하는지 매핑한다.
2. 리팩터링으로 깨질 가능성이 높지만 테스트가 없는 계약만 characterization test로 추가한다.
3. 중복 테스트를 무조건 늘리지 말고 테스트 이름과 Given-When-Then 구조를 정리한다.
4. H2 테스트와 MySQL Testcontainers 테스트의 책임을 문서화한다.
5. `FLOW_REGRESSION_MATRIX.md`에 흐름, API, 테스트 클래스, 기대 결과를 기록한다.

### 완료 조건

- 모든 핵심 흐름이 최소 하나의 자동 테스트 또는 명시적 수동 검증에 연결돼 있다.
- 기존 37개 테스트가 유지되고 필요한 회귀 테스트가 추가된다.
- 리팩터링 전 전체 테스트가 통과한다.

### 권장 커밋

```text
test: 핵심 사용자와 관리자 흐름 회귀 기준 고정
```

---

## 2단계: 백엔드 책임 경계 리팩터링

### 목표

서비스 클래스에 섞인 조회 조건 조립, 리소스 검증, 명령 처리와 DTO 변환 책임을 명확하게 분리한다. 트랜잭션과 쿼리 수는 기존보다 나빠지지 않아야 한다.

### 우선 점검 대상

- `PostServiceImpl`: 게시글 명령, 목록 검색, FULLTEXT 분기, 후속 기록, 접근 검증이 한 클래스에 집중됨
- `AdminServiceImpl`: 신고 처리, 게시글 moderation, 사용자 제한, 카테고리 관리, 운영 지표가 한 클래스에 집중됨
- `PostController`, `AdminController`: 페이지 정책과 응답 조립의 일관성
- `@Value` 기반 검색 설정: 타입 안전한 설정 객체로 통일 필요
- 시간 생성: 여러 서비스의 `LocalDateTime.now()` 직접 호출로 테스트 제어가 어려움

### 구현 방향

1. `SearchProperties` 같은 타입 안전한 `@ConfigurationProperties`로 검색 설정을 이동한다.
2. 공개 게시글 조회 조건과 정렬 생성 책임을 별도 검색 조건 객체 또는 팩터리로 추출한다.
3. 게시글 접근 검증은 공개 조회, 작성자 변경, 관리자 moderation의 차이를 명확히 표현한다.
4. `PostServiceImpl`은 유스케이스 조정 역할을 유지하고 검색 명세 조립과 반복 조회 코드를 작은 협력 객체로 분리한다.
5. 후속 기록 로직은 부모 게시글 상태와 소유권 검증 순서를 한 곳에서 보장한다.
6. `AdminServiceImpl`은 moderation, 사용자 관리, 카테고리 관리, 지표 조회 경계를 기준으로 분리하되 서비스 수를 불필요하게 늘리지 않는다.
7. 변경 시각이 정책 결과에 영향을 주는 곳은 Spring `Clock` 주입을 검토한다.
8. 정적 DTO 팩터리는 변환 규칙이 단순하면 유지한다. 매퍼 계층을 기계적으로 추가하지 않는다.
9. 쓰기 유스케이스의 `@Transactional`, 읽기 유스케이스의 `readOnly=true` 경계를 보존한다.
10. FULLTEXT ID 페이지 조회 후 EntityGraph 재조회 순서와 목록 SQL 회귀 기준을 보존한다.

### 금지 사항

- JPA 엔티티를 API 응답으로 직접 노출하지 않는다.
- 컨트롤러로 비즈니스 검증을 이동하지 않는다.
- 모든 서비스에 인터페이스를 새로 만들지 않는다.
- 검증되지 않은 캐시를 추가하지 않는다.
- 기존 원자적 공감·신고 집계와 잠금 순서를 바꾸지 않는다.

### 검증

```powershell
.\gradlew.bat test --tests com.fail.app.PostListIntegrationTest
.\gradlew.bat test --tests com.fail.app.PostMutationAuthorizationIntegrationTest
.\gradlew.bat test --tests com.fail.app.AdminReportModerationIntegrationTest
.\gradlew.bat test --tests com.fail.app.AdminUserModerationIntegrationTest
.\gradlew.bat test --tests com.fail.app.PostQueryPerformanceIntegrationTest
.\gradlew.bat test --rerun-tasks
```

### 완료 조건

- 주요 서비스의 책임과 의존성이 이전보다 명확하다.
- 공개 API 계약과 Flyway 스키마 변경이 없다.
- 게시글 목록 SQL 회귀 기준과 관리자 지표 SQL `2개` 기준이 유지된다.
- MySQL 동시성 테스트가 통과한다.
- 리팩터링 전후 구조 선택 근거를 `REFACTORING_LOG.md`에 기록한다.

### 권장 커밋

```text
refactor: 게시글과 관리자 유스케이스 책임 분리
```

---

## 3단계: 프론트엔드 상태와 API 경계 리팩터링

### 목표

페이지에 섞인 인증 확인, API 호출, 폼 상태, 임시 저장과 오류 처리를 분리해 읽기 쉽고 테스트 가능한 구조로 만든다.

### 우선 점검 대상

- `admin/page.tsx`: 타입, 인증, 병렬 조회, moderation, 카테고리 폼과 렌더링이 한 파일에 압축됨
- `PostForm.tsx`: 폼 상태, 소유권 확인, 카테고리 조회, localStorage 초안, 제출이 한 컴포넌트에 집중됨
- `PostDetailView.tsx`: 후속 기록, 공감, 신고와 상세 렌더링 책임 확인 필요
- `lib/api.ts`: 재시도와 오류 응답 처리의 재사용성, 429 `Retry-After` 전달 확인 필요
- 페이지별 반복되는 토큰 확인과 로그인 리다이렉트

### 구현 방향

1. 관리자 API, 게시글 API, 인증 API를 타입이 있는 도메인 함수로 분리한다.
2. 공통 `apiFetch`는 전송, 한 번의 토큰 갱신, 오류 변환 책임만 유지한다.
3. `ApiError`가 오류 코드와 `Retry-After`를 일관되게 전달할 수 있게 한다.
4. 반복 인증 확인은 작은 훅 또는 서버·클라이언트 경계에 맞는 공통 함수로 정리한다.
5. `admin/page.tsx`를 지표, 카테고리 관리, 신고 목록 컴포넌트로 분리한다.
6. `PostForm`의 초안 저장·복원은 버전이 있는 타입과 전용 훅으로 분리한다.
7. 손상된 localStorage 데이터, 오래된 초안 스키마, 저장 용량 오류를 안전하게 처리한다.
8. 제출 중 중복 요청 방지와 오류 메시지 표시를 모든 변경 폼에서 일관되게 유지한다.
9. CSS와 현재 시각 디자인은 유지한다. 리팩터링과 디자인 변경을 섞지 않는다.
10. React Compiler 지침을 따르고 필요 없는 `useMemo`, `useCallback`을 추가하지 않는다.

### 검증

```powershell
cd frontend
npm.cmd run lint
npm.cmd run build
```

브라우저에서 다음을 확인한다.

1. 로그인 만료 후 동시 요청이 갱신 API 하나로 합쳐진다.
2. 새 글 초안이 새로고침 후 복원된다.
3. 수정 글 초안과 새 글 초안이 섞이지 않는다.
4. 관리자 아닌 사용자는 관리자 화면에서 서비스 화면으로 이동한다.
5. 관리자 신고 숨김·복구와 카테고리 변경이 정상 동작한다.
6. 429 응답에서 재시도 안내가 표시된다.

### 완료 조건

- 관리자 페이지와 게시글 폼의 역할이 컴포넌트·훅·API 모듈 경계로 분리된다.
- 기존 라우트와 화면 디자인이 유지된다.
- lint와 production build가 통과한다.
- 프론트엔드 구조 변경 근거가 `REFACTORING_LOG.md`에 기록된다.

### 권장 커밋

```text
refactor: 프론트엔드 인증과 폼 상태 경계 정리
```

---

## 4단계: 문서·설정·코드 품질 정합성

### 목표

현재 코드와 맞지 않는 오래된 문서, 설정 중복과 품질 문제를 정리한다.

### 작업

1. `BACKEND_REVIEW_RESULT.md`의 해결 전 평가와 현재 상태를 명확히 구분한다.
2. README, 기능 현황, API 명세, 하네스의 테스트 수와 운영 정책을 현재 코드와 맞춘다.
3. `NEXT_WORK_PLAN.md`는 완료 계획으로 보존하고 새 리팩터링 계획과 연결한다.
4. 설정 속성은 가능한 범위에서 `@ConfigurationProperties`로 통일하고 유효성 검증을 추가한다.
5. 패키지 import 순서, 지나치게 긴 메서드, 한 줄로 압축된 TSX를 정리한다.
6. 사용하지 않는 코드, 타입, 환경 변수와 중복 문서를 제거하되 삭제 근거를 확인한다.
7. `git diff --check`, 문서 내부 링크, Compose 설정과 대시보드 JSON을 검증한다.

### 완료 조건

- 문서가 과거 문제를 현재 미해결 문제처럼 표현하지 않는다.
- 코드와 환경 변수 예시가 일치한다.
- 전체 테스트와 빌드가 통과한다.
- 작업 트리에 의도하지 않은 변경이 없다.

### 권장 커밋

```text
docs: 리팩터링 결과와 현재 운영 기준 동기화
```

---

## 5단계: 다음 작업으로 Playwright E2E 도입

이 단계는 1~4단계가 모두 통과한 뒤에만 시작한다.

### 목표

백엔드 통합 테스트만으로 확인하기 어려운 브라우저의 인증 저장, 쿠키 갱신, 라우팅, 폼 임시 저장과 관리자 화면 흐름을 자동 검증한다.

### 우선 E2E 시나리오

| 우선순위 | 시나리오 | 핵심 검증 |
| --- | --- | --- |
| P0 | 공개 탐색 | 메인 목록, 검색, 카테고리 필터, 상세 이동 |
| P0 | 인증과 게시글 | 로그인, 글 작성, 수정, 후속 기록, 내 기록 조회 |
| P0 | 관리자 moderation | 신고 조회, 게시글 숨김, 공개 목록 제외, 복구 |
| P1 | Refresh Token | Access Token 만료 후 자동 갱신과 원 요청 재시도 |
| P1 | 임시 저장 | 작성 중 새로고침 후 초안 복원과 발행 후 삭제 |

### 구현 기준

1. `frontend`에 Playwright를 구성한다.
2. 테스트마다 고유한 사용자나 재현 가능한 시드 데이터를 사용한다.
3. 테스트 간 실행 순서 의존성을 만들지 않는다.
4. 비밀번호, JWT, 쿠키를 테스트 로그나 스크린샷에 노출하지 않는다.
5. 실패 시 스크린샷과 trace를 CI artifact로 남긴다.
6. 로컬 실행 명령과 CI 실행 방법을 README에 추가한다.
7. CI 비용이 과도하면 PR에서는 핵심 3개만 실행하고 전체 시나리오는 수동 workflow로 분리한 근거를 문서화한다.

### 완료 조건

- P0 시나리오 3개가 로컬에서 독립적으로 통과한다.
- GitHub Actions에서 E2E가 재현된다.
- 실패 artifact로 원인을 확인할 수 있다.
- `DEMO_SCENARIO.md`의 대표 흐름과 E2E 시나리오가 연결된다.

### 권장 커밋

```text
test: 핵심 사용자와 관리자 Playwright E2E 추가
```

---

## 단계별 공통 검증

각 단계 종료 시 다음을 실행한다.

```powershell
.\gradlew.bat test
cd frontend
npm.cmd run lint
npm.cmd run build
```

Docker나 관측성 설정이 변경되면 다음도 실행한다.

```powershell
docker compose --profile observability --env-file .env.example config
docker compose --profile observability --env-file .env.example up -d --build
docker compose --profile observability --env-file .env.example ps
```

Prometheus 설정이 변경되면 `promtool check config`를 실행한다.

### 단계 종료 보고 형식

각 단계 종료 시 다음 다섯 항목만 간결하게 보고한다.

1. 발견한 구조 문제
2. 선택한 리팩터링과 선택 이유
3. 보존한 API·DB·화면 계약
4. 실행한 테스트와 결과
5. 생성한 커밋과 다음 단계

### 최종 완료 조건

- 1~5단계가 모두 완료됐다.
- 백엔드 전체 테스트, 프론트엔드 lint·build, Playwright P0 E2E가 통과한다.
- Docker Compose와 관측성 프로필이 정상 실행된다.
- 문서와 실제 코드가 일치한다.
- 단계별 커밋이 명확하다.
- `main`에 푸시한 뒤 GitHub Actions가 통과한다.
- 원격과 로컬 HEAD가 일치하고 작업 트리가 깨끗하다.

### 중단 기준

다음 상황에서만 사용자 입력을 요청한다.

- 기존 API 계약을 깨야만 해결 가능한 실제 결함이 발견됐다.
- 사용자 변경과 현재 작업이 같은 코드에서 충돌한다.
- 외부 계정, 배포 권한 또는 비용 발생 승인이 필요하다.
- 동일한 외부 환경 실패가 반복돼 로컬에서 더 진행할 수 없다.

그 외에는 합리적인 기본값을 선택하고 근거를 기록하면서 다음 단계로 계속 진행한다.

---

## 현재 점검에서 확인한 우선순위

| 우선순위 | 대상 | 이유 |
| --- | --- | --- |
| P0 | 핵심 흐름 회귀 매트릭스 | 리팩터링의 동작 보존 기준 필요 |
| P1 | `PostServiceImpl` 책임 분리 | 260줄 이상이며 검색·명령·후속 기록·검증이 혼재 |
| P1 | `AdminServiceImpl` 책임 분리 | moderation·사용자·카테고리·지표가 혼재 |
| P1 | `admin/page.tsx` 구조화 | 타입·인증·API·폼·렌더링이 압축된 한 파일에 집중 |
| P1 | `PostForm` 초안·인증 분리 | 폼 상태와 localStorage·소유권·API 책임이 혼재 |
| P2 | 설정 타입 안전성 | 검색 설정만 `@Value`로 남아 설정 정책이 불일치 |
| P2 | 문서 현재화 | 과거 리뷰 결과 일부가 현재 미해결 상태처럼 읽힘 |
| 다음 작업 | Playwright E2E | 브라우저 인증·쿠키·라우팅·임시 저장 흐름의 자동 근거 부족 |
