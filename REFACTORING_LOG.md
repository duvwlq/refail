# Re:Fail 리팩터링 기록

이 문서는 공개 API와 데이터 계약을 유지하면서 내부 책임을 정리한 과정과 검증 근거를 기록한다.

## 1단계: 핵심 흐름 회귀 기준 고정

### 발견한 문제

- 인증, 게시글, 후속 기록, 검색, 공감, 신고, 관리자, 관측성 흐름의 검증 위치가 여러 테스트에 흩어져 있었다.
- 중복 가입과 잘못된 비밀번호, 게시글 수정·삭제 권한처럼 리팩터링 중 깨지기 쉬운 계약 일부가 직접 검증되지 않았다.

### 선택한 변경

- `FLOW_REGRESSION_MATRIX.md`에 사용자 흐름과 API, 자동 테스트를 연결했다.
- 인증 실패 계약과 게시글·후속 기록의 작성자 권한 계약을 통합 테스트로 보강했다.
- 관리자 전용 메트릭의 비인증 접근 차단을 관측성 테스트에 추가했다.

### 보존한 계약

- REST 경로, HTTP 상태, 오류 코드와 JSON 응답 필드를 변경하지 않았다.
- Flyway 스키마와 공개 화면 동작을 변경하지 않았다.

### 검증

- 백엔드 전체 테스트 통과
- 프론트엔드 lint 및 production build 통과

### 커밋

- `149c16d test: 핵심 사용자와 관리자 흐름 회귀 기준 고정`

## 2단계: 백엔드 서비스 책임 분리

### 발견한 문제

- `PostServiceImpl`이 명령 처리와 공개 목록 조건 조립, FULLTEXT 분기, 내 기록 조회를 함께 담당했다.
- `AdminServiceImpl`이 권한 확인, 신고 처리, 게시글·사용자 조치, 카테고리 관리, 운영 지표 조회를 모두 담당했다.
- 검색 설정은 `@Value` 필드 주입을 사용했고 삭제·숨김 시각은 `LocalDateTime.now()`에 직접 의존했다.

### 선택한 변경

- `PostQueryService`로 공개 목록, FULLTEXT 검색, 내 기록 조회를 추출했다.
- `AdminAccessPolicy`가 관리자·관리 대상 접근 검증을 담당하도록 했다.
- `AdminCategoryManager`와 `AdminMetricsReader`로 카테고리 명령과 운영 지표 조회를 분리했다.
- `SearchProperties`로 검색 설정을 타입 안전하게 바인딩했다.
- 공통 `Clock` 빈을 주입해 시간 기반 명령을 테스트에서 대체할 수 있게 했다.
- 서비스 수가 과도하게 늘지 않도록 신고 목록과 게시글·사용자 moderation은 하나의 관리자 유스케이스 서비스에 유지했다.

### 보존한 계약

- `PostService`, `AdminService`, 컨트롤러의 공개 메서드와 REST 계약을 유지했다.
- 목록의 EntityGraph, 후속 기록 일괄 조회, FULLTEXT ID 페이지 조회 순서를 유지했다.
- 쓰기 유스케이스의 트랜잭션과 조회 유스케이스의 `readOnly` 경계를 유지했다.
- Flyway 스키마를 변경하지 않았다.

### 검증 기준

- 게시글 목록 SQL은 데이터 수와 무관하게 최대 3개를 유지한다.
- 관리자 운영 지표 SQL은 권한 확인을 포함해 최대 2개를 유지한다.
- H2 목록·권한·관리자 회귀 테스트와 MySQL FULLTEXT·동시성 테스트를 모두 통과해야 한다.

### 검증 결과

- 게시글 목록·수정 권한·관리자 신고·사용자 조치·운영 지표 통합 테스트 통과
- MySQL FULLTEXT와 핵심 동시성 통합 테스트 통과
- 백엔드 전체 테스트 통과
- 프론트엔드 lint 및 production build 통과

### 다음 단계

- 프론트엔드 인증, API, 페이지 상태, 임시 저장 책임을 분리한다.

## 3단계: 프론트엔드 상태와 API 경계 분리

### 발견한 문제

- 관리자 페이지가 인증, 데이터 조회, moderation, 카테고리 폼 상태를 한 파일에서 처리했다.
- `PostForm`이 인증, 소유권 확인, REST 경로, 초안 JSON 파싱과 저장을 모두 담당했다.
- 상세·인증·내 기록 화면이 API URL과 HTTP 메서드를 직접 조립했다.
- `ApiError`가 429 응답의 `Retry-After`를 화면에 전달하지 못했다.

### 선택한 변경

- `lib/api` 아래에 인증, 게시글, 카테고리, 관리자 도메인 API를 분리했다.
- 공통 `apiFetch`에는 전송, 한 번의 토큰 갱신, 오류 변환만 남겼다.
- `useRequireAuth`와 `useAccessTokenAction`으로 페이지 진입과 사용자 액션의 인증 규칙을 구분했다.
- 관리자 화면을 오케스트레이터, 지표, 카테고리, 신고 목록 컴포넌트로 분리했다.
- `usePostDraft`에 초안 버전과 런타임 검증을 추가해 손상되거나 오래된 초안을 안전하게 폐기한다.
- `ApiError`가 `retryAfterSeconds`를 제공하고 429 안내 문구에 재시도 시간을 포함하도록 했다.

### 보존한 계약

- 기존 URL, 폼 필드, CSS Module과 화면 구조를 유지했다.
- 새 글과 수정 글의 초안 키를 분리하고 500ms 자동 저장 간격을 유지했다.
- Access Token 만료 시 갱신 요청 하나를 공유하고 원래 요청을 한 번만 재시도하는 규칙을 유지했다.
- 관리자 권한이 없거나 로그인이 없을 때의 리다이렉트 경로를 유지했다.

### 검증 결과

- 프론트엔드 ESLint 통과
- Next.js production build와 TypeScript 검사 통과
- 브라우저에서 공개 검색 결과와 URL 쿼리 동기화 확인
- 로그아웃 상태에서 글 작성·관리자 화면의 로그인 리다이렉트 확인
- 브라우저 콘솔 오류 없음

### 다음 단계

- 문서와 현재 코드·운영 설정을 동기화한 뒤 Playwright P0 시나리오를 추가한다.

## 4단계: 문서·설정·코드 품질 정합성

### 발견한 문제

- 백엔드 리뷰 문서가 해결된 P1~P3 항목을 현재 위험과 다음 작업으로 계속 표시했다.
- README와 시연 문서의 자동 테스트 수가 37개로 남아 있었다.
- 일부 문서 링크가 로컬 절대 경로여서 GitHub에서 열리지 않았다.
- JWT와 요청 제한 설정은 타입 객체였지만 잘못된 값의 시작 시 검증이 없었다.
- 초안 JSON은 검증했지만 브라우저 저장소 접근·용량 예외는 처리하지 않았다.

### 선택한 변경

- 리뷰 당시 근거는 보존하고 최초 평가와 현재 해결 상태를 명확히 분리했다.
- 테스트 기준을 40개로 갱신하고 하네스·API·ERD 링크를 저장소 상대 경로로 통일했다.
- `JwtProperties`, `RateLimitProperties`에 Bean Validation 제약과 바인딩 실패 테스트를 추가했다.
- Compose와 `.env.example`의 관리 포트·내부 메트릭·SQL 로그 설정을 일치시켰다.
- `usePostDraft`가 저장소 접근 거부와 용량 초과를 안내하고 작성 흐름을 계속 유지하도록 했다.

### 보존한 계약

- REST API, JSON 응답, Flyway 스키마와 화면 디자인은 변경하지 않았다.
- 최초 코드 리뷰의 문제 발견 근거와 당시 수치는 역사 기록으로 유지했다.
- 관측성 관리 포트와 Prometheus 내부 수집 정책의 기본값을 유지했다.

### 검증 기준

- 백엔드 전체 테스트, 프론트엔드 lint·production build
- Markdown 내부 링크, `git diff --check`
- Compose observability 프로필 렌더링
- Grafana JSON과 Prometheus 설정 검증

### 검증 결과

- 백엔드 40개 테스트 통과
- 프론트엔드 ESLint와 Next.js production build 통과
- Markdown 내부 링크와 Grafana 대시보드 JSON 검증 통과
- `promtool` 설정 1개와 경보 규칙 3개 검증 통과
- Compose 5개 컨테이너 실행, 애플리케이션 3개 헬스 체크 정상
- Prometheus의 `backend:9090` 수집 대상 `up`

### 다음 단계

- Playwright P0 E2E 3개와 GitHub Actions 실행·artifact 보존을 구성한다.

## 5단계: Playwright P0 E2E와 CI

### 발견한 문제

- API 통합 테스트는 개별 계약을 보호했지만 실제 브라우저의 로그인부터 후속 기록까지 이어지는 흐름은 자동화되지 않았다.
- 내 기록 보관함이 백엔드 최대 페이지 크기 50을 넘는 `size=100`을 요청해 정상 작성 후에도 빈 상태처럼 보였다.
- CI는 백엔드 테스트와 프론트엔드 정적 검증만 실행해 Compose 환경의 프론트엔드·백엔드 연결을 검증하지 않았다.
- 프로덕션 의존성 감사에서 Next.js와 하위 PostCSS·Sharp의 high 취약점 3건이 확인됐다.

### 선택한 변경

- 공개 탐색, 작성자 CRUD·후속 기록, 관리자 신고 숨김·복구를 각각 독립된 P0 Playwright 명세로 만들었다.
- 테스트마다 API로 고유 사용자를 생성하고 고정 관리자만 별도 스크립트로 승격해 병렬 실행 간 데이터 충돌을 막았다.
- 내 기록 요청 크기를 서버 계약에 맞는 50으로 조정하고 조회 실패를 빈 목록과 구분해 표시했다.
- GitHub Actions가 Chromium과 Compose 환경을 준비하고, 실패 시 trace·영상·스크린샷·서버 로그를 artifact로 남기도록 했다.
- Next.js를 `16.2.11`로 올리고 PostCSS `8.5.22`, Sharp `0.35.3`을 고정해 프로덕션 의존성 취약점을 제거했다.

### 보존한 계약

- 공개 API 경로와 응답 구조, 인증·익명 정책, 관리자 처리 정책은 변경하지 않았다.
- E2E 전용 요청 제한 상향은 `compose.e2e.yaml`에 격리해 기본 운영 설정에 영향을 주지 않는다.
- 테스트 계정 비밀번호와 토큰은 로그에 출력하지 않는다.

### 검증 기준

- Playwright P0 시나리오 3개 전체 통과
- 백엔드 40개 테스트, 프론트엔드 lint·production build 통과
- Compose 애플리케이션과 관측성 구성 정상
- `npm audit --omit=dev` 취약점 0건
- GitHub Actions 전체 작업 통과

### 로컬 검증 결과

- 백엔드 22개 테스트 스위트, 40개 테스트 통과
- 프론트엔드 ESLint와 Next.js `16.2.11` production build 통과
- Playwright P0 시나리오 3개 병렬 실행 통과
- Docker `npm ci`와 production build 통과, 취약점 0건
- Compose 5개 서비스 실행과 Prometheus 수집 대상 `up`

## 후속 단계: HTTPS 운영 배포 경계

### 발견한 문제

- 개발 Compose가 MySQL, Spring Boot, Next.js를 각각 호스트 포트에 공개했다.
- API·브라우저 테스트는 통과했지만 실제 HTTPS 프록시에서 Secure Refresh Cookie와 동일 출처 API를 검증하지 않았다.
- 전달 IP 신뢰는 프록시가 외부 헤더를 무시하고 백엔드 직접 접근이 차단되는 조건과 함께 확인해야 했다.

### 선택한 변경

- Caddy를 유일한 HTTP·HTTPS 진입점으로 두고 API와 웹을 동일 출처로 제공했다.
- 운영 Compose override에서 DB·백엔드·프론트엔드·관리 포트의 호스트 매핑을 제거했다.
- 비루트 이미지, read-only 파일 시스템, capability 제거와 보안 응답 헤더를 적용했다.
- HTTPS 인증 수명주기와 포트 경계를 10단계 스모크와 GitHub Actions로 자동화했다.

### 검증 결과

- MySQL, Spring Boot, Next.js, Caddy 네 서비스 `healthy`
- Caddy의 HTTP `18000`, HTTPS `18443` 외 테스트 프로젝트 공개 포트 없음
- HTTP 리다이렉트, HTTPS SSR·헬스·Swagger 비노출과 보안 헤더 통과
- Secure Refresh Cookie 발급·회전, 게시글 작성, 로그아웃 후 갱신 `401` 통과
- 테스트 컨테이너·네트워크·볼륨 자동 정리

실제 외부 도메인 인증서와 서버 방화벽은 클라우드·DNS 자격 증명이 필요하므로 로컬 스모크 결과와 구분한다.
