# Re:Fail 핵심 흐름 회귀 매트릭스

## 1. 목적

이 문서는 구조 리팩터링 전후에 유지해야 하는 사용자·관리자 흐름과 자동 검증 근거를 연결한다. API 경로, 상태 코드, 오류 코드, JSON 필드, 공개 상태와 데이터 정합성은 리팩터링 중 변경하지 않는다.

기준 시점은 `86ced4b` 이후이며, 리팩터링 전 백엔드 전체 테스트와 프론트엔드 lint·production build가 통과한 상태다.

## 2. 흐름별 검증 근거

| 흐름 | 보존할 계약 | 자동 검증 |
| --- | --- | --- |
| 회원가입 | 정상 가입 `200`, 이메일 중복 `409/USER_003`, 닉네임 중복 `409/USER_004` | `AuthSessionIntegrationTest.signupRejectsDuplicateIdentityAndLoginRejectsInvalidCredentials` |
| 로그인 | 정상 로그인 시 Access Token과 HttpOnly Refresh 쿠키 발급, 잘못된 인증 `401/AUTH_003` | `AuthSessionIntegrationTest` |
| 토큰 갱신·로그아웃 | 갱신 시 Refresh 회전, 이전 토큰 거부, 로그아웃 후 갱신 거부 | `AuthSessionIntegrationTest.loginRefreshRotationAndLogoutManageTheSessionLifecycle` |
| 토큰 경쟁 조건 | 동시 갱신 뒤 유효한 토큰 흐름 하나만 유지 | `MySqlCoreIntegrationTest.concurrentRefreshRequestsLeaveOneActiveTokenOnMySql` |
| 게시글 작성·소유권 | 인증 사용자 작성, 작성자 여부만 반환하고 익명 작성자 ID 비공개 | `PostMutationAuthorizationIntegrationTest` |
| 게시글 수정·삭제 | 비로그인 `401`, 비작성자 `403`, 작성자 변경 성공, 삭제 후 `404` | `PostMutationAuthorizationIntegrationTest` |
| 후속 기록 CRUD | 작성자만 작성·수정·삭제, 삭제된 후속 기록 제외 | `PostMutationAuthorizationIntegrationTest` |
| 숨김·삭제 부모의 후속 기록 | 공개 상세·후속 목록 모두 `404/POST_001` | `AdminReportModerationIntegrationTest`, `PostMutationAuthorizationIntegrationTest` |
| 목록 탐색 | 카테고리·인기순, 후속 기록 여부, 숨김·삭제 제외 | `PostListIntegrationTest` |
| 검색 | 제목·본문·감정 태그, 카테고리·크기 결합, 공개 글만 반환 | `MySqlCoreIntegrationTest.fullTextSearchFindsKoreanTermsAndExcludesHiddenPostsOnMySql` |
| 목록 입력 상한 | 페이지 최대 50, 게시글 검색어 최대 100자, 카테고리 검색어 최대 50자 | `ListInputValidationIntegrationTest` |
| 공감 | 본인 글 `403`, 생성·조회·변경·취소와 집계 일치 | `ReactionReportFlowIntegrationTest` |
| 신고 | 생성 성공, 동일 사용자 중복 `409/REPORT_001`, 집계 일치 | `ReactionReportFlowIntegrationTest` |
| 공감·신고 동시성 | 실제 행 수와 부모 집계 값 일치, 잠금 순서 유지 | `MySqlCoreIntegrationTest.concurrentReactionsAndReportsKeepExactAggregateCountsOnMySql` |
| 관리자 신고 처리 | 비관리자 `403`, 신고 조회, 숨김·복구, 신고 해결과 감사 이력 | `AdminReportModerationIntegrationTest` |
| 관리자 사용자 제한 | 제한·활성화 이력 대상이 `USER`, 제한 시 Refresh Token 폐기 | `AdminUserModerationIntegrationTest`, `MySqlCoreIntegrationTest` |
| 제한 관리자 | 기존 Access Token으로 관리자 조회·변경 불가 | `AdminReportModerationIntegrationTest` |
| 비활성 카테고리 | 새 분류 거부, 기존 게시글 공개 유지 | `UserCategoryPolicyIntegrationTest` |
| 요청 제한 | 로그인·가입·갱신은 IP, 신고는 사용자 ID, `429`와 `Retry-After` | `RateLimitIntegrationTest` |
| 요청 추적 | 모든 응답에 Request ID, 유효한 상위 ID 보존 | `RequestObservabilityIntegrationTest` |
| 메트릭 보안 | 기본 모드는 비인증 메트릭 `401`, 관리자는 조회 가능, 내부 수집 모드는 명시적으로 허용 | `RequestObservabilityIntegrationTest`, `InternalMetricsSecurityIntegrationTest` |
| 스키마 | Flyway 전체 적용과 JPA validate 성공 | `MySqlCoreIntegrationTest.flywayCreatesAndValidatesTheSchemaOnMySql` |
| 쿼리 수 | 게시글 수가 늘어도 목록 SQL 최대 3개, 관리자 지표 전체 요청 SQL 2개 | `PostQueryPerformanceIntegrationTest`, `AdminMetricsQueryIntegrationTest` |

## 3. 테스트 계층의 책임

### H2 기반 Spring 통합 테스트

- HTTP 경로, 인증·권한, 상태 코드, 오류 코드와 JSON 계약을 빠르게 검증한다.
- 서비스 상태 정책과 공개 노출 조건을 검증한다.
- 목록 SQL 회귀처럼 데이터베이스 종류와 무관한 구조적 회귀를 검증한다.

### MySQL 8.4 Testcontainers 테스트

- Flyway SQL과 MySQL 스키마 호환성을 검증한다.
- 유니크 제약 경쟁 조건과 InnoDB 잠금 동작을 검증한다.
- 공감·신고·Refresh Token 동시성 정합성을 검증한다.
- ngram FULLTEXT 검색과 실제 공개 상태 필터를 검증한다.

### 프론트엔드 정적 검증

- ESLint로 React·TypeScript 규칙 위반을 확인한다.
- Next.js production build로 라우트, 타입과 서버·클라이언트 경계를 확인한다.
- 브라우저 쿠키, 라우팅, 임시 저장과 관리자 화면 연결은 5단계 Playwright E2E에서 자동화한다.

## 4. 리팩터링 단계별 필수 회귀

| 변경 범위 | 필수 검증 |
| --- | --- |
| 게시글 조회·검색 | `PostListIntegrationTest`, `PostQueryPerformanceIntegrationTest`, MySQL FULLTEXT 테스트 |
| 게시글·후속 기록 명령 | `PostMutationAuthorizationIntegrationTest`, 숨김·삭제 부모 테스트 |
| 관리자 서비스 | 관리자 신고·사용자·지표 테스트 |
| 인증·설정 | 인증 수명주기, JWT, 요청 제한 테스트 |
| 프론트엔드 API·상태 | lint, production build, 관련 Playwright 시나리오 |
| Docker·관측성 | Compose config, 전체 컨테이너 health, Prometheus target과 Grafana provisioning |

새 테스트는 결함 위험이나 계약 공백을 설명할 수 있을 때만 추가한다. 같은 경로를 단순 반복하는 테스트 개수 증가는 목표가 아니다.
