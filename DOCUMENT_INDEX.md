# Re:Fail 문서 안내

## 포트폴리오 작성

- `PORTFOLIO_IMPROVEMENTS.md`: 문제, 선택, 구현, 검증, 성과를 모은 포트폴리오용 개선 기록
- `PORTFOLIO_POINTS.md`: 문제 정의와 핵심 정책, MVP, 기술 선택 근거
- `FEATURE_STATUS.md`: 현재 구현된 기능과 시연 계정

## 기획

- `PLAN.md`: 전체 서비스 기획
- `MVP_SPEC.md`: MVP 범위와 제외 기능

## 개발

- `API_SPEC.md`: REST API 명세
- `BACKEND_STRUCTURE.md`: Spring Boot 프로젝트 구조
- `ERD.md`: 엔티티와 관계
- `DATABASE.md`: MySQL·Flyway 실행 방법
- `SWAGGER.md`: OpenAPI 문서 사용 방법
- `AUTH_SECURITY.md`: JWT Access Token과 회전형 Refresh Token 보안 정책
- `RATE_LIMIT_POLICY.md`: 로그인·회원가입·갱신·신고 API 요청 제한 정책
- `OBSERVABILITY.md`: Prometheus·Grafana 대시보드, 경보와 관리 포트 보안 정책
- `DEPLOYMENT.md`: Caddy HTTPS 운영 토폴로지, 실행·업데이트·백업·복구·롤백
- `ARCHITECTURE.md`: 시스템 구조, JWT 수명주기, 공감·신고 동시성 흐름
- `DEMO_SCENARIO.md`: 회원가입부터 관리자 처리까지 대표 시연 순서
- `DEMO_VIDEO_SCRIPT.md`: 3~5분 포트폴리오 영상 대본과 촬영 체크리스트

## 품질과 운영

- `PERFORMANCE.md`: N+1 개선, 캐시, 인덱스, 측정 결과
- `HARNESS.md`: 구현과 검증 시 지켜야 할 기준
- `NEXT_WORK_PLAN.md`: 인증 수명주기, 검색 성능, 요청 제한, 관측성의 다음 단계 실행 계획
- `REFACTORING_GOAL_PROMPT.md`: 전체 흐름 점검, 구조 리팩터링과 Playwright E2E까지 이어지는 목표 모드 프롬프트
- `REFACTORING_LOG.md`: 단계별 구조 개선 선택, 보존 계약, 검증 결과
- `DEPLOYMENT_GOAL_PROMPT.md`: HTTPS 단일 진입점과 운영 스모크를 완성하는 목표 모드 프롬프트
- `FLOW_REGRESSION_MATRIX.md`: 리팩터링 전후 보존할 핵심 흐름과 자동 테스트 근거
- `BACKEND_REVIEW_PROMPT.md`: 백엔드 포트폴리오의 부족한 점을 코드 근거로 점검하는 리뷰 프롬프트
- `BACKEND_REVIEW_RESULT.md`: 리뷰 프롬프트를 실제 코드와 실행 API에 적용한 발견 사항과 우선순위

포트폴리오나 README를 작성할 때는 `PORTFOLIO_IMPROVEMENTS.md`를 먼저 보고, 세부 근거가 필요할 때 나머지 문서를 참고한다.
