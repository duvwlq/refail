# Swagger/OpenAPI 사용 가이드

## 접속 주소

백엔드를 실행한 뒤 다음 주소에서 API 문서를 확인한다.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- 헬스 체크: `http://localhost:8080/api/v1/health`

Docker 프로필은 백엔드 포트가 `18080`이므로 주소의 포트를 `18080`으로 변경한다.

## JWT 인증 방법

1. `POST /api/v1/auth/signup`으로 사용자를 등록한다.
2. `POST /api/v1/auth/login`을 실행한다.
3. 응답의 `accessToken` 값을 복사한다.
4. Swagger UI 상단의 `Authorize` 버튼을 누른다.
5. 토큰 값만 입력하고 인증한다.

Swagger가 `Bearer` 접두사를 자동으로 추가하므로 입력란에는 액세스 토큰만 넣는다.

## 헬스 체크 응답

정상 상태에서는 HTTP `200`과 다음 구조를 반환한다.

```json
{
  "status": "UP",
  "application": "UP",
  "database": "UP",
  "checkedAt": "2026-07-13T14:00:00"
}
```

애플리케이션은 실행 중이지만 데이터베이스 연결 확인에 실패하면 HTTP `503`을 반환하고 `status`, `database` 값이 `DOWN`이 된다.
