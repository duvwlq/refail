# Re:Fail 포트폴리오 주장 근거표

## 1. 목적

포트폴리오의 각 주장이 코드, 자동 테스트와 측정 문서로 증명되는 범위를 정의한다. 근거보다 넓은 표현은 사용하지 않으며, 외부 환경에서 검증하지 않은 항목은 향후 계획으로 구분한다.

## 2. 근거표

| 주장 | 코드·설정 | 자동 테스트 | 측정·문서 | 표현 가능한 범위 |
| --- | --- | --- | --- | --- |
| Java 21과 Spring Boot 3.5.3 사용 | `build.gradle` | Gradle build | [README](README.md) | 현재 저장소 기술 버전 |
| 게시글 목록 SQL 최대 3회 | `PostRepository.java`, `PostQueryService.java` | `PostQueryPerformanceIntegrationTest` | [성능 설계](PERFORMANCE.md) | 게시글 10건 목록의 준비된 SQL 수 |
| 게시글·신고 목록 N+1 제거 | `PostRepository.java`, `ReportRepository.java`의 `EntityGraph` | `PostQueryPerformanceIntegrationTest`, 관리자 신고 통합 테스트 | [개선 기록](PORTFOLIO_IMPROVEMENTS.md) | 현재 목록 응답 변환 경로 |
| 5만 건 본문 검색 평균 `72.47ms → 6.57ms` | `V7__add_post_fulltext_index.sql`, `PostRepository.java` | `SearchPerformanceBenchmarkTest` | [검색 벤치마크](PERFORMANCE.md) | 2026-07-24 로컬 MySQL 8.4, 3회 워밍업·10회 측정 |
| 검색 p95 `105.49ms → 7.84ms` | 동일 | 동일 | [검색 벤치마크](PERFORMANCE.md) | 동일 데이터 생성 규칙의 본문 `알고리즘` 시나리오 |
| 관리자 지표 API SQL `7회 → 2회` | `PostRepository.java`, `AdminMetricsReader.java` | `AdminMetricsQueryIntegrationTest` | [성능 설계](PERFORMANCE.md) | 관리자 상태 확인을 포함한 API SQL 수 |
| 관리자 집계 평균 `6.75ms → 3.18ms` | `PostRepository.java`, `AdminMetricsReader.java` | 벤치마크 재현 코드 | [성능 설계](PERFORMANCE.md) | 게시글 1만·후속 기록 1천·신고 200건의 로컬 MySQL 측정 |
| 공감·신고 카운터 lost update 방지 | `PostRepository.java`, `ReactionServiceImpl.java`, `ReportServiceImpl.java` | `ReactionReportConcurrencyIntegrationTest`, `MySqlCoreIntegrationTest` | [아키텍처](ARCHITECTURE.md) | 구현된 원자적 증감과 자동 정합성 검증 |
| 공감·신고 각 8개 동시 요청 정합성 | 동일 | `MySqlCoreIntegrationTest` | [백엔드 리뷰 결과](BACKEND_REVIEW_RESULT.md) | MySQL 8.4에서 카운터와 실제 행 개수 일치 |
| InnoDB 교착 상태 해결 | 부모·자식 변경 순서를 통일한 서비스 코드 | `MySqlCoreIntegrationTest` | [개선 기록](PORTFOLIO_IMPROVEMENTS.md) | 현재 테스트 시나리오의 잠금 순서 회귀 방지 |
| 15분 Access Token | `JwtProperties.java`, `application.yml` | 인증 통합 테스트 | [인증 정책](AUTH_SECURITY.md) | 현재 설정의 Access Token TTL |
| Refresh Token SHA-256 해시 저장 | `RefreshTokenService.java`, `RefreshToken.java` | `AuthSessionIntegrationTest` | [인증 정책](AUTH_SECURITY.md) | DB에 원문 대신 해시 저장 |
| Refresh Token 회전·재사용 탐지 | `RefreshTokenService.java`, `RefreshTokenRepository.java` | `AuthSessionIntegrationTest`, `MySqlCoreIntegrationTest` | [인증 정책](AUTH_SECURITY.md) | 갱신 회전, 중복·재사용 처리와 계열 폐기 |
| 제한 관리자 기존 JWT 차단 | `AdminAccessPolicy.java`와 관리자 유스케이스 | 관리자 moderation 통합 테스트 | [백엔드 리뷰 결과](BACKEND_REVIEW_RESULT.md) | DB의 현재 역할·상태 검증 후 `403` |
| 사용자 검색어별 무제한 캐시 제거 | `CategoryServiceImpl.java`, 캐시 설정 | `CategoryCacheIntegrationTest` | [백엔드 리뷰 결과](BACKEND_REVIEW_RESULT.md) | 활성 목록 단일 캐시 키와 변경 시 무효화 |
| MySQL·Flyway 자동 검증 | `MySqlContainerIntegrationSupport.java`, `db/migration` | `MySqlCoreIntegrationTest` | [데이터베이스 가이드](DATABASE.md) | Testcontainers MySQL 8.4의 독립 스키마 |
| 백엔드 테스트 40개 통과 | `src/test` | `gradlew test` | [README](README.md) | 최신 전체 실행 결과를 다시 확인한 뒤 사용 |
| Playwright P0 3개 | `frontend/e2e/*.spec.ts` | `npm run test:e2e` | [시연 시나리오](DEMO_SCENARIO.md) | 공개·작성자·관리자 핵심 브라우저 흐름 |
| Caddy HTTPS 단일 진입점 | `compose.production.yaml`, `ops/caddy/*` | `test-production-deployment.ps1` | [운영 배포](DEPLOYMENT.md) | 로컬·CI 운영형 Compose의 공개 포트 경계 |
| HTTPS 운영 스모크 10단계 | `smoke-production.mjs`, `test-production-deployment.ps1` | GitHub Actions `production-smoke` | [운영 배포](DEPLOYMENT.md) | 내부 CA 기반 HTTPS 인증·프록시 흐름 |
| 요청 ID와 처리 시간 관측 | 요청 관측 필터, Micrometer 설정 | `RequestObservabilityIntegrationTest` | [관측성 가이드](OBSERVABILITY.md) | 애플리케이션 요청 로그와 메트릭 |

## 3. 완료형으로 표현하지 않는 항목

| 항목 | 현재 근거 | 포트폴리오 표현 |
| --- | --- | --- |
| 실제 외부 배포 | 로컬·CI 운영형 Compose만 검증 | `실제 도메인 배포는 다음 단계` |
| 무중단 배포 | 단일 서버 재빌드·재기동 절차 | 사용하지 않음 |
| 대용량 트래픽 처리 | 5만 건 검색 벤치마크와 8개 동시 정합성 테스트 | `로컬 데이터 기준 비교 측정` |
| 고가용성 | 단일 서버 구조 | 사용하지 않음 |
| 장시간 부하 안정성 | 검증 없음 | `장시간 부하 테스트 필요` |
| 백업 복구 시간 | 백업·복구 절차 문서만 존재 | `실제 복구 훈련 필요` |
| 운영 사용자 성과 | 실제 사용자·트래픽 데이터 없음 | 사용하지 않음 |
| 팀 협업 성과 | 개인 프로젝트 | `개인 프로젝트 담당 범위` |

## 4. 최신 검증 기록

이 표의 테스트 개수와 CI 결과는 포트폴리오 문서 갱신 시마다 다시 확인한다.

| 검증 | 최신 결과 | 증거 |
| --- | --- | --- |
| 백엔드 전체 테스트 | 40개 성공, 실패·오류·건너뜀 0 | 2026-07-24 `gradlew test --rerun-tasks`, XML 22개 스위트 |
| 프론트엔드 lint | 성공 | 2026-07-24 `npm run lint` |
| 프론트엔드 production build | 성공 | 2026-07-24 Next.js 16.2.11 `npm run build` |
| Playwright P0 3개 | 원격 CI 확인 예정 | 현재 포트폴리오 커밋의 GitHub Actions |
| HTTPS 운영 스모크 10단계 | 원격 CI 확인 예정 | 현재 포트폴리오 커밋의 GitHub Actions |
| 원격 CI | 푸시 후 확인 예정 | 현재 포트폴리오 커밋의 GitHub Actions URL |
