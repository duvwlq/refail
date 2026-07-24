# Re:Fail 한 페이지 포트폴리오

## 프로젝트

**Re:Fail - 실패를 공유하고, 다음을 준비하다**

성공 중심 SNS의 비교 피로를 문제로 정의하고 실패와 후속 성장 과정을 안전하게 기록하는 서비스입니다.

GitHub: [github.com/duvwlq/refail](https://github.com/duvwlq/refail)

## 담당 역할

개인 프로젝트로 서비스 기획, API·DB 설계, 백엔드·프론트엔드 구현, 테스트, Docker 실행 환경과 GitHub Actions CI를 구성했습니다.

## 핵심 성과

| 영역 | 성과 |
| --- | --- |
| 검색 | 로컬 MySQL 8.4의 5만 건 본문 검색 평균 `72.47ms → 6.57ms` |
| 조회 | 목록 SQL 최대 3회, 관리자 지표 API SQL `7회 → 2회` |
| 정합성 | 공감·신고 각각 8개 동시 요청의 카운터와 실제 행 개수 일치 |
| 운영 | Caddy HTTPS 단일 진입점과 인증·포트 경계 10단계 CI 스모크 |

## 대표 문제 해결

### 1. 검색 전체 스캔 개선

`%LIKE%`가 5만 건을 스캔하는 문제에 MySQL ngram FULLTEXT를 적용했습니다. 동일 결과 건수로 3회 워밍업 후 10회 측정해 본문 검색 평균을 `72.47ms`에서 `6.57ms`로 약 90.9% 줄였습니다. 현재 규모에서는 Elasticsearch의 인프라와 동기화 비용보다 MySQL 활용이 적절하다고 판단했습니다.

### 2. 동시성 정합성 확보

공감·신고 카운터의 lost update를 DB 원자적 UPDATE로 방지했습니다. 실제 MySQL 테스트에서 발견한 부모·자식 행 교착은 잠금 순서를 통일해 해결했고, Testcontainers MySQL 8.4에서 각각 8개 동시 요청의 집계와 실제 행 개수 일치를 자동 검증했습니다.

### 3. 인증과 운영 경계 검증

15분 Access Token과 해시 저장·회전형 Refresh Token을 구현하고 재사용 시 토큰 계열을 폐기했습니다. Caddy만 HTTPS 진입점으로 공개하고 내부 서비스 포트를 제거했으며, Secure Cookie 회전부터 로그아웃 폐기까지 10단계 운영 스모크를 GitHub Actions에서 실행합니다.

## 기술 스택

`Java 21` `Spring Boot 3.5.3` `Spring Data JPA` `Spring Security` `MySQL 8.4` `Flyway` `Testcontainers` `Next.js 16` `TypeScript` `Docker Compose` `Caddy` `Prometheus` `Grafana` `GitHub Actions`

## 검증과 한계

백엔드 40개, Playwright P0 3개와 HTTPS 운영 스모크 10단계를 자동화했습니다. 성능 수치는 로컬 Docker 환경의 비교 측정이며 실제 운영 트래픽 결과가 아닙니다. 공개 도메인 배포, 외부 백업 복구와 장시간 부하 검증은 다음 단계입니다.
