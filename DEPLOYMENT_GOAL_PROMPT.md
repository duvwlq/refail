# Re:Fail 운영 배포 고도화 목표 모드 실행 프롬프트

아래 프롬프트 전체를 Codex 목표 모드에 사용한다.

---

## 실행 프롬프트

### 최종 목표

Re:Fail을 무료·저비용 단일 서버에 배포할 수 있는 운영형 구조로 고도화한다. 외부에는 HTTPS 게이트웨이 하나만 노출하고 MySQL, Spring Boot, Next.js, 관리 포트는 Docker 내부 네트워크에서만 접근하게 한다. 브라우저는 동일 출처로 API를 호출하고 Refresh Token의 `Secure`, `HttpOnly`, `SameSite` 정책이 실제 HTTPS 프록시 환경에서 동작해야 한다.

구성 파일만 추가하는 것으로 끝내지 않는다. 운영 위협 모델, 프록시 신뢰 경계, 자동 배포 스모크 테스트, CI, 실행·복구 문서와 포트폴리오 근거까지 완성한다. 실제 클라우드 계정이나 도메인 자격 증명이 없으면 외부 배포를 요구하지 않되, 같은 이미지를 서버에서 재현할 수 있는 상태까지 진행한다.

### 현재 기준선

- 저장소: `duvwlq/refail`
- 기준 브랜치: `main`
- 기준 커밋: `b7418bf` 이후 최신 `origin/main`
- 백엔드: Java 21, Spring Boot 3.5, MySQL 8.4, Flyway
- 프론트엔드: Next.js 16.2.11, React 19, TypeScript
- 자동 검증: 백엔드 40개 테스트, 프론트엔드 lint·production build, Playwright P0 3개
- 현재 Compose: MySQL, Spring Boot, Next.js, 선택형 Prometheus·Grafana
- 현재 한계: 개발 Compose가 MySQL·백엔드·프론트엔드 포트를 호스트에 각각 노출하고, 실제 HTTPS 프록시·Secure 쿠키 흐름은 자동 검증하지 않는다.

### 반드시 지킬 원칙

1. 저장소와 관련 문서를 먼저 읽고 실제 코드와 Docker Compose 기능을 기준으로 계획을 수정한다.
2. 계획만 작성하고 멈추지 말고 구현, 검증, 문서화, 독립 커밋, 원격 CI 확인까지 진행한다.
3. 개발용 `compose.yaml`은 기존 로컬 개발 흐름을 보존하고 운영 차이는 별도 Compose 파일로 명시한다.
4. 운영 구성에서는 게이트웨이 외의 애플리케이션·DB 포트를 호스트에 공개하지 않는다.
5. 브라우저 API는 동일 출처를 사용하고 서버 렌더링만 Docker 내부 백엔드 주소를 사용한다.
6. 운영 Refresh Cookie는 `Secure`, `HttpOnly`, 명시적 `SameSite`를 유지한다.
7. 전달 IP 헤더를 신뢰할 때는 백엔드 직접 접근이 차단되고 게이트웨이가 외부 입력 헤더를 덮어쓰는 조건을 함께 보장한다.
8. 운영 시크릿을 저장소, 이미지, CI 로그, 테스트 결과에 출력하지 않는다.
9. 실제 도메인과 클라우드 자격 증명이 없으면 임의 값을 만들거나 외부 배포 성공을 주장하지 않는다.
10. Kubernetes, Redis, 메시지 큐, 서비스 분리는 현재 단일 서버 목표에 필요하지 않으므로 추가하지 않는다.
11. 각 단계 종료 시 회귀 테스트를 수행하고 의미 있는 단위로 커밋한다.
12. 전체 완료는 작업 트리 clean, 로컬·원격 HEAD 일치, GitHub Actions 성공까지 포함한다.

### 시작 전 필수 확인

다음 문서를 읽는다.

- `README.md`
- `ARCHITECTURE.md`
- `AUTH_SECURITY.md`
- `RATE_LIMIT_POLICY.md`
- `OBSERVABILITY.md`
- `HARNESS.md`
- `API_SPEC.md`
- `PORTFOLIO_IMPROVEMENTS.md`
- `REFACTORING_LOG.md`
- `DEMO_SCENARIO.md`

다음 파일과 코드를 확인한다.

- `compose.yaml`, `compose.e2e.yaml`, `.env.example`
- 백엔드·프론트엔드 `Dockerfile`
- Refresh Cookie 생성 코드
- CORS와 프록시 IP 처리 코드
- 프론트엔드 공개·내부 API Base URL 처리
- GitHub Actions와 Playwright 설정

기준 상태를 확보한다.

```powershell
git status --short
git log -10 --oneline
.\gradlew.bat test --rerun-tasks
cd frontend
npm.cmd run lint
npm.cmd run build
npm.cmd run test:e2e
```

예상하지 못한 사용자 변경이 있으면 덮어쓰지 않는다. 기준 테스트 실패는 원인을 먼저 분석한다.

---

## 1단계: 운영 위협 모델과 배포 계약 고정

### 목표

운영 배포에서 보호할 자산, 신뢰 경계, 공개 포트와 검증 항목을 구현 전에 문서와 테스트 기준으로 고정한다.

### 작업

1. 현재 Compose의 공개 포트, 네트워크 경로, 쿠키·CORS·프록시 설정을 표로 정리한다.
2. 외부 사용자, HTTPS 게이트웨이, 프론트엔드, 백엔드, 관리 포트, DB의 신뢰 경계를 정의한다.
3. 다음 운영 계약을 `DEPLOYMENT.md`에 기록한다.
   - 외부 공개: HTTP 리다이렉트와 HTTPS 게이트웨이만 허용
   - 내부 전용: MySQL, Spring Boot API·management, Next.js, Prometheus·Grafana
   - 브라우저 요청: 동일 출처 `/api/*`
   - SSR 요청: Docker 내부 `http://backend:8080`
   - Refresh Cookie: HTTPS에서만 전송
   - Swagger: 운영 기본 비활성화
4. 프록시 신뢰, 시크릿 주입, 백업·복구, 로그·메트릭 접근의 남은 위험을 명시한다.
5. 운영 Compose를 검사할 자동 검증 항목을 먼저 정의한다.

### 완료 조건

- 공개·내부 포트와 데이터 흐름이 모호하지 않다.
- 프록시 헤더를 신뢰할 수 있는 전제 조건이 문서화된다.
- 구현 후 자동 확인할 스모크 체크 목록이 존재한다.

### 권장 커밋

```text
docs: 운영 배포 위협 모델과 검증 계약 정의
```

---

## 2단계: HTTPS 단일 진입점과 운영 Compose

### 목표

Caddy를 HTTPS 게이트웨이로 사용해 프론트엔드와 API를 동일 출처로 제공하고 내부 서비스 직접 노출을 제거한다.

### 구현 방향

1. 운영 전용 Compose 파일을 추가한다.
2. 운영 파일에서 MySQL, 백엔드, 프론트엔드의 호스트 포트를 제거한다.
3. Caddy만 HTTP·HTTPS 포트를 공개한다.
4. `/api/*`는 백엔드, 그 외 요청은 프론트엔드로 전달한다.
5. 게이트웨이는 외부의 `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host`를 신뢰하지 않고 검증된 값으로 전달한다.
6. 브라우저용 `NEXT_PUBLIC_API_BASE_URL`은 동일 출처가 되게 하고 SSR의 내부 API 주소는 유지한다.
7. 운영 백엔드는 Secure Refresh Cookie와 신뢰 프록시 IP 정책을 사용한다.
8. HSTS, `X-Content-Type-Options`, frame 차단, Referrer Policy 등 호환 가능한 보안 헤더를 적용한다.
9. 컨테이너는 가능한 범위에서 비루트 사용자, `no-new-privileges`, 불필요 capability 제거, read-only 파일 시스템 또는 임시 파일 시스템을 사용한다.
10. 운영에 필요한 데이터만 명시적 볼륨으로 유지한다.
11. 로컬 검증은 Caddy의 내부 CA 또는 별도 검증 설정을 사용하되 운영 정책을 약화시키지 않는다.

### 금지 사항

- 운영 백엔드나 DB 포트를 디버깅 편의를 이유로 공개하지 않는다.
- 프론트엔드 이미지에 운영 도메인을 하드코딩하지 않는다.
- 자체 서명 인증서를 실제 운영 인증서처럼 문서화하지 않는다.
- 검증 없이 과도하게 엄격한 CSP를 적용해 Next.js 실행을 깨뜨리지 않는다.

### 완료 조건

- HTTPS 단일 주소에서 메인 화면과 `/api/v1/health`가 동작한다.
- HTTP는 HTTPS로 리다이렉트된다.
- 호스트에서 MySQL·백엔드·프론트엔드 포트에 직접 접근할 수 없다.
- 운영 응답에 정의한 보안 헤더가 존재한다.
- 운영 Compose의 모든 필수 서비스가 정상 상태다.

### 권장 커밋

```text
feat: HTTPS 단일 진입점 운영 Compose 구성
```

---

## 3단계: 인증·프록시·배포 스모크 자동화

### 목표

운영과 같은 HTTPS 경로에서 핵심 인증과 콘텐츠 흐름, 비공개 포트, 보안 헤더를 한 명령으로 검증한다.

### 필수 스모크 흐름

1. HTTP에서 HTTPS 리다이렉트
2. HTTPS 메인 화면과 공개 헬스 체크
3. 운영 Swagger 비노출
4. 회원가입과 로그인
5. 로그인 응답의 Refresh Cookie 속성 확인
6. Refresh Cookie를 이용한 Access Token 갱신
7. 인증 게시글 생성과 공개 조회
8. 로그아웃 후 Refresh 실패
9. MySQL·백엔드·프론트엔드 직접 호스트 포트 비노출
10. 보안 헤더와 Request ID 확인

### 작업

1. 테스트 데이터가 매 실행마다 충돌하지 않게 고유 값을 사용한다.
2. 토큰, 쿠키, 비밀번호를 표준 출력에 표시하지 않는다.
3. 실패 시 단계와 HTTP 상태만 진단 가능하게 출력한다.
4. PowerShell에서 재현 가능한 스모크 스크립트를 작성한다.
5. GitHub Actions에 독립 운영 배포 스모크 작업을 추가한다.
6. 실패 시 Caddy·프론트엔드·백엔드 로그를 비밀 제거 후 artifact로 보존한다.
7. 성공·실패와 관계없이 Compose 볼륨과 컨테이너를 정리한다.

### 완료 조건

- 로컬에서 스모크 스크립트 전체 통과
- GitHub Actions Linux 환경에서 같은 흐름 통과
- 실패 로그에 Access Token, Refresh Token, 비밀번호가 노출되지 않음

### 권장 커밋

```text
test: HTTPS 운영 배포 스모크 검증 자동화
```

---

## 4단계: 운영 문서와 포트폴리오 근거 동기화

### 목표

처음 보는 사람이 단일 서버에서 안전하게 실행·점검·복구할 수 있고, 구현 선택을 포트폴리오에서 설명할 수 있게 한다.

### 작업

1. `.env.production.example`에 필수 변수와 안전한 생성 기준을 작성한다.
2. `DEPLOYMENT.md`에 다음 내용을 완성한다.
   - 사전 요구사항
   - DNS·방화벽·Docker 설치 전제
   - 최초 실행과 업데이트
   - 헬스 체크와 로그 확인
   - DB 백업·복구 절차
   - 인증서 갱신 책임
   - 장애 시 롤백
   - 최소 비용 단일 서버 선택 근거
3. `README.md`, `ARCHITECTURE.md`, `AUTH_SECURITY.md`, `HARNESS.md`, `DOCUMENT_INDEX.md`를 동기화한다.
4. `PORTFOLIO_IMPROVEMENTS.md`에 문제 → 선택 → 검증 → 결과 → 한계를 기록한다.
5. stale한 “다음 작업” 문서를 현재 상태로 갱신한다.
6. 실제 외부 URL이 없으면 로컬 검증 주소와 “외부 배포 미수행”을 명확히 구분한다.

### 완료 조건

- 문서의 명령을 새 환경에서 순서대로 실행할 수 있다.
- 모든 성과 주장이 테스트나 구성 파일에 연결된다.
- 무료·저비용 운영의 장점뿐 아니라 단일 장애점과 백업 책임도 설명한다.

### 권장 커밋

```text
docs: 운영 배포와 복구 절차 포트폴리오 근거 정리
```

---

## 5단계: 전체 회귀와 원격 완료

### 필수 검증

```powershell
.\gradlew.bat test --rerun-tasks
cd frontend
npm.cmd run lint
npm.cmd run build
npm.cmd run test:e2e
```

추가 검증:

- `docker compose config --quiet`
- 개발 Compose와 운영 Compose 각각 기동
- 운영 HTTPS 배포 스모크 전체 통과
- Prometheus 수집 대상 `up`
- `npm audit --omit=dev` 취약점 0건
- `actionlint` 통과
- Markdown 내부 링크와 `git diff --check` 통과

### 완료 조건

1. 기존 REST·화면·DB 계약 회귀가 없다.
2. 백엔드 40개 이상 테스트와 Playwright P0 3개가 통과한다.
3. 운영 HTTPS 스모크가 로컬과 GitHub Actions에서 통과한다.
4. 운영 구성에서 게이트웨이 이외의 호스트 포트가 없다.
5. 작업 트리가 clean이다.
6. 단계별 커밋이 `main`에 푸시돼 있다.
7. 로컬 HEAD와 `origin/main`이 일치한다.
8. GitHub Actions 전체 작업이 성공한다.

### 최종 보고 형식

- 구현한 운영 토폴로지
- 자동 검증 결과와 수치
- 커밋 목록과 GitHub Actions 링크
- 실제 외부 배포 여부
- 남은 운영 위험과 다음 작업 한 가지

---

## 작업 중단 기준

다음 상황에서는 위험한 우회를 하지 말고 원인과 안전한 대안을 문서화한다.

- 외부 클라우드·DNS 자격 증명이 필요한 작업
- 로컬 CA 신뢰 설치처럼 시스템 전역 변경이 필요한 작업
- 운영 시크릿 원문을 저장소나 로그에 넣어야만 진행되는 작업
- Docker 또는 CI 플랫폼 제약으로 동일 검증을 재현할 수 없는 작업

이 경우에도 로컬 Compose, 자동 스모크, 문서화처럼 자격 증명 없이 가능한 작업은 계속 완료한다.
