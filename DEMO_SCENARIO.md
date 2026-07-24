# Re:Fail 대표 시연 시나리오

## 1. 준비

```powershell
Copy-Item .env.example .env
docker compose --profile observability --env-file .env up -d --build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\seed-demo-data.ps1
```

시연 계정:

| 역할 | 이메일 | 비밀번호 |
| --- | --- | --- |
| 사용자 | `demo@refail.local` | `password123` |
| 관리자 | `admin@refail.local` | `password123` |

## 2. 핵심 사용자 흐름

1. 메인 화면에서 다이어트, 공부, 일상 등 서로 다른 크기의 실패 기록을 보여준다.
2. 카테고리 `공부`와 검색어를 적용해 탐색 기능을 보여준다.
3. 사용자 계정으로 로그인한다.
4. 마크다운으로 실패 글을 작성하고 익명 또는 닉네임 공개 방식을 선택한다.
5. 선택 항목인 `다음에는 어떤 선택을 해볼 것인가`를 입력한다.
6. 상세 화면에서 마크다운 렌더링과 공감 유형별 집계를 보여준다.
7. 기존 실패 글에 `다시 시도 중` 또는 `극복함` 후속 기록을 작성한다.
8. 내 기록 보관함에서 익명 글도 작성자 본인이 관리할 수 있음을 보여준다.

## 3. 안전과 관리자 흐름

1. 다른 사용자의 게시글을 신고한다.
2. 동일 사용자의 중복 신고가 차단되는 정책을 설명한다.
3. 관리자 계정으로 로그인해 대기 신고와 사유를 확인한다.
4. 게시글을 숨기고 공개 목록에서 사라지는 것을 확인한다.
5. 복구 후 다시 공개되는 것을 확인한다.
6. 처리자, 처리 시점, 사유가 감사 이력에 남는 구조를 설명한다.

## 4. 백엔드 강조 흐름

1. Access Token 15분, Refresh Token 해시 저장·회전·재사용 탐지를 설명한다.
2. 공감·신고 집계가 DB 원자적 UPDATE이며 MySQL 동시성 테스트로 보호됨을 보여준다.
3. 5만 건 본문 검색 평균 `72.47ms → 6.57ms` 측정 결과를 보여준다.
4. 관리자 지표 SQL `7개 → 2개` 개선 결과를 보여준다.
5. Grafana에서 API 요청, p95 지연, JVM Heap, DB 풀을 보여준다.
6. GitHub Actions의 백엔드 테스트, 프론트엔드 빌드, Playwright P0 검증을 보여준다.

## 5. 자동화된 P0 시나리오

| 시연 흐름 | Playwright 명세 |
| --- | --- |
| 공개 검색·카테고리 필터·상세 이동 | `frontend/e2e/public-exploration.spec.ts` |
| 로그인·작성·수정·후속 기록·내 기록 | `frontend/e2e/auth-post-flow.spec.ts` |
| 신고 확인·숨김·공개 제외·복구 | `frontend/e2e/admin-moderation.spec.ts` |

## 6. 운영 배포 근거

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-production-deployment.ps1
```

1. 운영 Compose에서 Caddy만 HTTP·HTTPS 포트를 공개하는 결과를 보여준다.
2. HTTP 리다이렉트와 HTTPS 보안 헤더를 보여준다.
3. 로그인 응답의 Secure Refresh Cookie 속성을 자동 검증한다고 설명한다.
4. 갱신 회전, 게시글 작성, 로그아웃 후 토큰 폐기까지 10단계가 통과하는 것을 보여준다.
5. 로컬 내부 CA는 공개 인증서가 아니며 실제 외부 배포는 별도 자격 증명이 필요하다고 명시한다.

## 7. 장애 추적 예시

1. API 응답의 `X-Request-ID`를 확인한다.
2. 같은 ID가 백엔드 로그에 기록되는 것을 확인한다.
3. Grafana에서 오류율 또는 p95 이상을 확인한다.
4. 요청 ID로 해당 로그를 찾아 상태 코드와 처리 시간을 확인한다.

이 흐름은 메트릭으로 이상을 발견하고 요청 ID로 원인을 좁히는 최소 관측성 시나리오다.
