# Re:Fail API 요청 제한 정책

## 1. 목적

로그인 대입 공격, 반복 가입, 과도한 토큰 갱신, 반복 신고가 인증·서비스·데이터베이스까지 계속 전달되는 것을 막는다.

## 2. 기본 제한

| API | 식별 기준 | 기본 제한 |
| --- | --- | --- |
| `POST /api/v1/auth/login` | 클라이언트 IP | 60초당 5회 |
| `POST /api/v1/auth/signup` | 클라이언트 IP | 1시간당 3회 |
| `POST /api/v1/auth/refresh` | 클라이언트 IP | 60초당 10회 |
| `POST /api/v1/posts/{postId}/reports` | 인증 사용자 ID | 1시간당 5회 |

고정 윈도우 방식이며 제한 초과 요청은 서비스와 DB에 전달하지 않는다.

## 3. 응답

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 42
Content-Type: application/json
```

```json
{
  "code": "RATE_001",
  "message": "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.",
  "timestamp": "2026-07-24T01:00:00"
}
```

## 4. 메모리 정책

- 단일 서버에 맞춰 `ConcurrentHashMap` 기반 인메모리 제한기를 사용한다.
- 기본 최대 키 수는 10,000개다.
- 용량에 도달하면 만료된 키를 먼저 제거한다.
- 그래도 가득 차면 종료 시간이 가장 가까운 윈도우부터 제거한다.
- 다중 서버로 전환하면 서버별 제한이 달라지므로 Redis 같은 공유 저장소 기반 제한기로 교체한다.

## 5. 프록시 IP 정책

기본값은 `X-Forwarded-For`를 신뢰하지 않고 실제 연결의 `remoteAddr`를 사용한다.

`RATE_LIMIT_TRUST_FORWARDED_FOR=true`는 로드 밸런서나 리버스 프록시가 외부 요청의 기존 헤더를 제거하고 올바른 값을 다시 설정하는 환경에서만 사용한다. 그렇지 않으면 사용자가 헤더를 조작해 제한을 우회할 수 있다.

## 6. 관측성

제한 초과는 다음 Micrometer 메트릭으로 기록한다.

```text
refail.rate_limit.exceeded
```

`endpoint` 태그는 `login`, `signup`, `refresh`, `report` 중 하나다. IP, 사용자 ID, 이메일은 메트릭 태그에 포함하지 않는다.

## 7. 테스트

- `RateLimitIntegrationTest`: API별 경계값, `429`, `Retry-After`, 메트릭
- `InMemoryRateLimiterTest`: 최대 키 수를 넘지 않는지 검증

